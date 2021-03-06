package ai.evolv;

import ai.evolv.exceptions.AscendKeyError;
import ai.evolv.generics.GenericClass;

import com.google.gson.JsonArray;

import java.util.HashSet;
import java.util.Set;

class Execution<T> {

    private final String key;
    private final T defaultValue;
    private final AscendAction function;

    private Set<String> alreadyExecuted = new HashSet<>();

    Execution(String key, T defaultValue, AscendAction<T> function) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.function = function;
    }

    String getKey() {
        return key;
    }

    void executeWithAllocation(JsonArray rawAllocations) throws AscendKeyError {
        GenericClass<T> cls = new GenericClass(defaultValue.getClass());
        Allocations allocations = new Allocations(rawAllocations);
        T value = allocations.getValueFromGenome(key, cls.getMyType());

        Set<String> activeExperiments = allocations.getActiveExperiments();
        if (alreadyExecuted.isEmpty() || !alreadyExecuted.equals(activeExperiments)) {
            // there was a change to the allocations after reconciliation, apply changes
            function.apply(value);
        }

        alreadyExecuted = activeExperiments;
    }

    void executeWithDefault() {
        function.apply(defaultValue);
    }

}
