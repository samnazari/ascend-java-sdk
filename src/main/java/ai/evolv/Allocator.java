package ai.evolv;

import ai.evolv.exceptions.AscendRuntimeException;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.concurrent.CompletableFuture;


class Allocator {

    private static Logger logger = LoggerFactory.getLogger(Allocator.class);

    enum AllocationStatus {
        FETCHING, RETRIEVED, FAILED
    }

    private final ExecutionQueue executionQueue;
    private final AscendAllocationStore store;
    private final AscendConfig config;
    private final AscendParticipant ascendParticipant;
    private final EventEmitter eventEmitter;
    private final HttpClient httpClient;

    private boolean confirmationSandbagged = false;
    private boolean contaminationSandbagged = false;

    private AllocationStatus allocationStatus;

    Allocator(AscendConfig config) {
        this.executionQueue = config.getExecutionQueue();
        this.store = config.getAscendAllocationStore();
        this.config = config;
        this.ascendParticipant = config.getAscendParticipant();
        this.httpClient = config.getHttpClient();
        this.allocationStatus = AllocationStatus.FETCHING;
        this.eventEmitter = new EventEmitter(config);

    }

    AllocationStatus getAllocationStatus() {
        return allocationStatus;
    }

    void sandBagConfirmation() {
        confirmationSandbagged = true;
    }

    void sandBagContamination() {
        contaminationSandbagged = true;
    }

    String createAllocationsUrl() {
        try {
            String path = String.format("//%s/%s/%s/allocations", config.getDomain(), config.getVersion(),
                    config.getEnvironmentId());
            String queryString = String.format("uid=%s&sid=%s", ascendParticipant.getUserId(),
                    ascendParticipant.getSessionId());
            URI uri = new URI(config.getHttpScheme(), null, path, queryString, null);
            URL url = uri.toURL();

            return url.toString();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return "";
        }
    }

    CompletableFuture<JsonArray> fetchAllocations() {
        CompletableFuture<String> responseFuture = httpClient.get(createAllocationsUrl());

        return responseFuture.thenApply(responseBody -> {
            JsonParser parser = new JsonParser();
            JsonArray allocations = parser.parse(responseBody).getAsJsonArray();

            JsonArray previousAllocations = store.get();
            if (allocationsNotEmpty(previousAllocations)) {
                allocations = Allocations.reconcileAllocations(previousAllocations, allocations);
            }

            store.put(allocations);
            allocationStatus = AllocationStatus.RETRIEVED;

            if (confirmationSandbagged) {
                eventEmitter.confirm(allocations);
            }

            if (contaminationSandbagged) {
                eventEmitter.contaminate(allocations);
            }

            // could throw an exception due to customer's action logic
            try {
                executionQueue.executeAllWithValuesFromAllocations(allocations);
            } catch(Exception e) {
                throw new AscendRuntimeException(e);
            }

            return allocations;
        }).handle((result, ex) -> {
            if (ex != null && ex.getCause() instanceof AscendRuntimeException) {
                // surface any customer implementation errors
                logger.error(ex.getCause().getCause().toString());
                return result;
            } else if (ex != null && result == null ){
                return resolveAllocationFailure();
            } else {
                return result;
            }
        });
    }

    JsonArray resolveAllocationFailure() {
        logger.warn("There was an error while making an allocation request.");

        JsonArray allocations = store.get();
        if (allocationsNotEmpty(allocations)) {
            logger.warn("Falling back to participant's previous allocation.");

            if (confirmationSandbagged) {
                eventEmitter.confirm(allocations);
            }

            if (contaminationSandbagged) {
                eventEmitter.contaminate(allocations);
            }

            allocationStatus = AllocationStatus.RETRIEVED;
            executionQueue.executeAllWithValuesFromAllocations(allocations);
        } else {
            logger.warn("Falling back to the supplied defaults.");

            allocationStatus = AllocationStatus.FAILED;
            executionQueue.executeAllWithValuesFromDefaults();

            allocations = new JsonArray();
        }

        return allocations;
    }

    static boolean allocationsNotEmpty(JsonArray allocations) {
        return allocations != null && allocations.size() > 0;
    }



}
