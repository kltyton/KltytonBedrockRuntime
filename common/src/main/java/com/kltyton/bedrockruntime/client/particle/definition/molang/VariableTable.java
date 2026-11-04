package com.kltyton.bedrockruntime.client.particle.definition.molang;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.UndefinedValueScope;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangStruct;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class VariableTable {
    protected static final Object NO_LOCAL_FIELD = new Object();
    private static final ThreadLocal<Map<Map<String, Variable>, Set<String>>> RESOLVING =
            ThreadLocal.withInitial(IdentityHashMap::new);
    public final Map<String, Variable> table;
    protected VariableTable parent;
    private long parentRevision;
    private final Map<String, VariableTable> sharedFields;
    private final Set<String> structuredDefaults;
    private @Nullable Map<MathValue, InitialValue> initialValues;
    private record InitialValue(Object value) {}

    public VariableTable(Map<String, Variable> table, @Nullable VariableTable parent) {
        this.table = table;
        this.parent = parent;
        this.sharedFields = new LinkedHashMap<>();
        this.structuredDefaults = new LinkedHashSet<>();
    }

    protected VariableTable(Map<String, Variable> table, @Nullable VariableTable parent, VariableTable bindingsOwner) {
        this.table = table;
        this.parent = parent;
        this.sharedFields = bindingsOwner.sharedFields;
        this.structuredDefaults = bindingsOwner.structuredDefaults;
    }

    /** Keeps explicit initial-value snapshots in this instance, outside inherited variable bindings. */
    @ApiStatus.Internal
    public Object initialValue(MathValue definition, MolangInstance owner, Function<MolangInstance, Object> evaluator) {
        if (initialValues == null) initialValues = new IdentityHashMap<>();
        InitialValue value = initialValues.get(definition);
        if (value == null) {
            value = new InitialValue(evaluator.apply(owner));
            initialValues.put(definition, value);
        }
        return value.value();
    }

    @ApiStatus.Internal
    public void shareStructureField(String name, VariableTable source) {
        if (source == this) throw new IllegalArgumentException("Cannot share a field with the same variable table");
        sharedFields.put(name, source);
    }

    @ApiStatus.Internal
    public boolean hasSharedStructureField(String name) {
        return sharedFieldSource(name) != null;
    }

    @ApiStatus.Internal
    public boolean hasStructuredDefault(String name) {
        return structuredDefaults.contains(name) && table.containsKey(name);
    }

    @ApiStatus.Internal
    public void installStructuredDefault(String name, Variable variable) {
        table.put(name, variable);
        structuredDefaults.add(name);
    }

    protected @Nullable String structuredDefaultSource(String name) {
        if (structuredDefaults.isEmpty()) return null;
        if (hasStructuredDefault(name)) return name;
        int separator = name.lastIndexOf('.');
        while (separator > name.indexOf('.')) {
            String prefix = name.substring(0, separator);
            if (hasStructuredDefault(prefix)) return prefix;
            separator = name.lastIndexOf('.', separator - 1);
        }
        return null;
    }

    private @Nullable VariableTable sharedFieldSource(String name) {
        if (sharedFields.isEmpty()) return null;
        VariableTable source = sharedFields.get(name);
        if (source != null) return source;
        int separator = name.lastIndexOf('.');
        while (separator > name.indexOf('.')) {
            source = sharedFields.get(name.substring(0, separator));
            if (source != null) return source;
            separator = name.lastIndexOf('.', separator - 1);
        }
        return null;
    }

    public VariableTable(@Nullable VariableTable parent) {
        this(new Hashtable<>(), parent);
    }

    public double getValue(String name, MolangInstance instance) {
        Object field = getLocalStructureField(name, instance);
        if (field != NO_LOCAL_FIELD) {
            if (field == null && !containsVariable(name)) return UndefinedValueScope.missingNumber();
            return field instanceof Number number ? number.doubleValue() : 0.0;
        }
        Variable variable = table.get(name);
        if (variable == null) {
            if (parent == null) {
                if (UndefinedValueScope.isActive() && getStructureSnapshot(name, instance) != null) return 0.0;
                return UndefinedValueScope.missingNumber();
            }
            return parent.getValue(name, instance);
        }
        if (!beginResolve(name)) return 0.0;
        try {
            return variable.get(instance);
        } finally {
            endResolve(name);
        }
    }

    public double getLocalValue(String name, MolangInstance instance) {
        Variable variable = table.get(name);
        if (variable == null || !beginResolve(name)) return 0.0;
        try {
            return variable.get(instance);
        } finally {
            endResolve(name);
        }
    }

    public @Nullable Object getLocalRawValue(String name, MolangInstance instance) {
        Variable variable = table.get(name);
        if (variable == null || !beginResolve(name)) return null;
        try {
            return variable.getRaw(instance);
        } finally {
            endResolve(name);
        }
    }

    public @Nullable Object getRawValue(String name, MolangInstance instance) {
        MolangStruct sharedStructure = getSharedStructureSnapshot(name, instance);
        if (sharedStructure != null) return sharedStructure;
        Object field = getLocalStructureField(name, instance);
        if (field != NO_LOCAL_FIELD) {
            if (field == null && !containsVariable(name)) return UndefinedValueScope.missing();
            return field;
        }
        Variable variable = table.get(name);
        if (variable == null) {
            if (parent != null) return parent.getRawValue(name, instance);
            if (UndefinedValueScope.isActive()) {
                MolangStruct structure = getStructureSnapshot(name, instance);
                if (structure != null) return structure;
            }
            return UndefinedValueScope.missing();
        }
        if (!beginResolve(name)) return null;
        try {
            return variable.getRaw(instance);
        } finally {
            endResolve(name);
        }
    }

    @ApiStatus.Internal
    public @Nullable MolangStruct getStructureSnapshot(String name, MolangInstance instance) {
        MolangStruct result = new MolangStruct();
        return mergeStructureFields(name, instance, result, false) ? result : null;
    }

    @ApiStatus.Internal
    public boolean hasStructureFields(String name) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) return shared.hasStructureFields(name);
        String structured = structuredDefaultSource(name);
        int rootEnd = name.startsWith("variable.") ? name.indexOf('.', "variable.".length()) : -1;
        String rootName = rootEnd < 0 ? name : name.substring(0, rootEnd);
        Variable root = structured != null ? table.get(structured) : table.get(rootName);
        boolean ownsRoot = root != null;
        if (ownsRoot) {
            Object value = root.rawReference();
            String owner = structured != null ? structured : rootName;
            if (!owner.equals(name)) value = structurePath(value, name.substring(owner.length() + 1));
            if (value instanceof MolangStruct structure && !structure.fieldNames().isEmpty()) return true;
            if (value instanceof Map<?, ?> fields && !fields.isEmpty()) return true;
        }
        String prefix = name + ".";
        for (String field : table.keySet()) {
            if (field.startsWith(prefix) && (!ownsRoot || structuredDefaults.contains(field))
                    && sharedFieldSource(field) == null) return true;
        }
        for (var binding : sharedFields.entrySet()) {
            String field = binding.getKey();
            if (field.startsWith(prefix) && (binding.getValue().containsVariable(field)
                    || binding.getValue().hasStructureFields(field))) return true;
        }
        return !ownsRoot && parent != null && parent.hasStructureFields(name);
    }

    protected @Nullable MolangStruct getSharedStructureSnapshot(String name, MolangInstance instance) {
        return hasLocalStructureOverlays(name) ? getStructureSnapshot(name, instance) : null;
    }

    @ApiStatus.Internal
    public boolean hasLocalStructureOverlays(String name) {
        String prefix = name + ".";
        for (String field : structuredDefaults) {
            if (field.startsWith(prefix)) return true;
        }
        for (String field : sharedFields.keySet()) {
            if (field.startsWith(prefix)) return true;
        }
        return false;
    }

    @ApiStatus.Internal
    public boolean mergeStructureFields(String name, MolangInstance instance, MolangStruct result, boolean present) {
        if (parent != null) present = parent.mergeStructureFields(name, instance, result, present);
        return mergeLocalStructureFields(name, instance, result, present);
    }

    @ApiStatus.Internal
    public boolean mergeLocalStructureFields(String name, MolangInstance instance, MolangStruct result, boolean present) {
        Object root = getLocalStructureField(name, instance);
        boolean ownsRoot = root != NO_LOCAL_FIELD || table.containsKey(name);
        if (ownsRoot) {
            if (root == NO_LOCAL_FIELD) root = getLocalRawValue(name, instance);
            result.clear();
            present = false;
            if (root instanceof MolangStruct structure) {
                result.merge(structure);
                present = true;
            } else if (root instanceof Map<?, ?> values) {
                result.merge(MolangStruct.fromMap(values));
                present = true;
            }
        }
        String prefix = name + ".";
        var fields = ownsRoot ? java.util.List.<String>of() : table.keySet().stream()
                .filter(key -> key.startsWith(prefix) && sharedFieldSource(key) == null && !structuredDefaults.contains(key))
                .sorted(java.util.Comparator.comparingInt(String::length).thenComparing(java.util.Comparator.naturalOrder()))
                .toList();
        for (String field : fields) {
            if (!table.containsKey(field)) continue;
            result.putPath(field.substring(prefix.length()), getLocalRawValue(field, instance));
            present = true;
        }
        var defaultFields = structuredDefaults.stream().sorted(java.util.Comparator.comparingInt(String::length)).toList();
        for (String field : defaultFields) {
            if (field.startsWith(prefix) && table.containsKey(field)) {
                result.putPath(field.substring(prefix.length()), getLocalStructureField(field, instance));
                present = true;
            }
        }
        var bindings = sharedFields.entrySet().stream()
                .filter(binding -> binding.getKey().startsWith(prefix))
                .sorted(java.util.Comparator.comparingInt(binding -> binding.getKey().length())).toList();
        for (var binding : bindings) {
            String field = binding.getKey();
            VariableTable source = binding.getValue();
            Object value;
            if (source.hasStructureFields(field)) {
                value = source.getStructureSnapshot(field, instance);
            } else if (source.containsVariable(field)) {
                value = source.getRawValue(field, instance);
            } else continue;
            result.putPath(field.substring(prefix.length()), value);
            present = true;
        }
        return present;
    }

    public @Nullable String getStringValue(String name, MolangInstance instance) {
        Object value = getRawValue(name, instance);
        return value instanceof String string ? string : null;
    }

    public @Nullable Variable getVariable(String name) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) return shared.getVariable(name);
        String structured = structuredDefaultSource(name);
        if (structured != null) return structured.equals(name) ? table.get(name) : null;
        int rootEnd = name.startsWith("variable.") ? name.indexOf('.', "variable.".length()) : -1;
        if (rootEnd >= 0 && table.containsKey(name.substring(0, rootEnd))) return null;
        Variable variable = table.get(name);
        if (variable != null) return variable;
        return variable != null || parent == null ? variable : parent.getVariable(name);
    }

    public boolean containsVariable(String name) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) return shared.containsVariable(name);
        String structured = structuredDefaultSource(name);
        if (structured != null) {
            return structured.equals(name) || table.get(structured).containsStructureField(name.substring(structured.length() + 1));
        }
        int rootEnd = name.indexOf('.', name.indexOf('.') + 1);
        if (name.startsWith("variable.") && rootEnd >= 0) {
            Variable root = table.get(name.substring(0, rootEnd));
            if (root != null) return root.containsStructureField(name.substring(rootEnd + 1));
        }
        if (table.containsKey(name)) return true;
        int namespaceEnd = name.indexOf('.');
        int separator = name.lastIndexOf('.');
        while (separator > namespaceEnd) {
            Variable root = table.get(name.substring(0, separator));
            Object value = root == null ? null : root.structure();
            if (value instanceof MolangStruct) {
                String[] fields = name.substring(separator + 1).split("\\.");
                boolean found = true;
                for (String field : fields) {
                    if (!(value instanceof MolangStruct structure) || !structure.contains(field)) {
                        found = false;
                        break;
                    }
                    value = structure.get(field);
                }
                if (found) return true;
            }
            separator = name.lastIndexOf('.', separator - 1);
        }
        return parent != null && parent.containsVariable(name);
    }

    public void setValue(String name, ToDoubleFunction<MolangInstance> function) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) {
            shared.setValue(name, function);
            return;
        }
        Variable variable = table.get(name);
        if (hasStructureRoot(name) && !hasStructuredDefault(name)) {
            if (variable == null) installStructuredDefault(name, new Variable(name, function));
            else {
                variable.set(function);
                structuredDefaults.add(name);
            }
            return;
        }
        if (variable == null) {
            table.put(name, new Variable(name, function));
        } else {
            variable.set(function);
        }
    }

    public void setValue(String name, double value) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) {
            shared.setRawValue(name, value);
            return;
        }
        String structured = structuredDefaultSource(name);
        if (structured != null && !structured.equals(name)) {
            setStructuredDefaultPath(structured, name, value);
            return;
        }
        if (structured == null && shouldWriteStructure(name)) {
            setRawValueLocal(name, value);
            return;
        }
        Variable variable = table.get(name);
        if (variable == null) {
            table.put(name, new Variable(name, value));
        } else {
            variable.set(value);
        }
    }

    public void setValue(String name, String value) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) {
            shared.setRawValue(name, value);
            return;
        }
        String structured = structuredDefaultSource(name);
        if (structured != null && !structured.equals(name)) {
            setStructuredDefaultPath(structured, name, value);
            return;
        }
        if (structured == null && shouldWriteStructure(name)) {
            setRawValueLocal(name, value);
            return;
        }
        Variable variable = table.get(name);
        if (variable == null) {
            table.put(name, new Variable(name, value));
        } else {
            variable.set(value);
        }
    }

    public void setRawValue(String name, Object value) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) {
            shared.setRawValue(name, value);
            return;
        }
        String structured = structuredDefaultSource(name);
        if (structured != null) {
            setStructuredDefaultPath(structured, name, value);
            return;
        }
        writeSharedDescendants(name, value, null);
        setRawValueLocal(name, value);
    }

    private void setRawValueLocal(String name, Object value) {
        int firstField = localStructureRootEnd(name);
        if (name.startsWith("variable.") && firstField >= 0
                && (!table.containsKey(name) || table.containsKey(name.substring(0, firstField)))) {
            String rootName = name.substring(0, firstField);
            Variable rootVariable = table.get(rootName);
            if (rootVariable == null && parent != null) {
                Variable inherited = parent.getVariable(rootName);
                MolangStruct structure = inherited == null ? null : inherited.structure();
                if (structure == null && inherited != null && inherited.rawReference() instanceof Map<?, ?> map) {
                    structure = MolangStruct.fromMap(map);
                }
                if (structure != null) {
                    rootVariable = new Variable(rootName, 0.0);
                    rootVariable.setRaw(structure.copy());
                    table.put(rootName, rootVariable);
                }
            }
            MolangStruct rootValue = rootVariable == null ? null : rootVariable.structure();
            MolangStruct structure;
            if (rootValue != null) structure = rootValue;
            else {
                Object reference = rootVariable == null ? null : rootVariable.rawReference();
                structure = reference instanceof Map<?, ?> map ? MolangStruct.fromMap(map) : new MolangStruct();
                Variable root = table.computeIfAbsent(rootName, key -> new Variable(key, 0.0));
                root.setRaw(structure);
            }
            String[] fields = name.substring(firstField + 1).split("\\.");
            for (int i = 0; i < fields.length - 1; i++) structure = structure.child(fields[i]);
            structure.put(fields[fields.length - 1], value);
            return;
        }
        if (value instanceof Number number) {
            setValue(name, number.doubleValue());
        } else if (value instanceof String string) {
            setValue(name, string);
        } else {
            Variable variable = table.computeIfAbsent(name, key -> new Variable(key, 0.0));
            variable.setRaw(value instanceof MolangStruct structure ? structure.copy() : value);
        }
    }

    public void setRawValue(String name, Object value, MolangInstance instance) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) {
            shared.setRawValue(name, value, instance);
            return;
        }
        String structured = structuredDefaultSource(name);
        if (structured != null) {
            setStructuredDefaultPath(structured, name, value);
            return;
        }
        writeSharedDescendants(name, value, instance);
        int firstField = localStructureRootEnd(name);
        if (name.startsWith("variable.") && firstField >= 0
                && (!table.containsKey(name) || table.containsKey(name.substring(0, firstField)))) {
            String rootName = name.substring(0, firstField);
            if (!table.containsKey(rootName)) {
                Object inherited = containsVariable(rootName) ? getRawValue(rootName, instance) : null;
                if (!(inherited instanceof MolangStruct)) inherited = getStructureSnapshot(rootName, instance);
                if (inherited instanceof MolangStruct structure) setRawValueLocal(rootName, structure);
            }
        }
        setRawValueLocal(name, value);
    }

    private void writeSharedDescendants(String name, Object value, @Nullable MolangInstance instance) {
        if ((sharedFields.isEmpty() && structuredDefaults.isEmpty())
                || !(value instanceof MolangStruct || value instanceof Map<?, ?>)) return;
        String prefix = name + ".";
        var defaultFields = structuredDefaults.stream().sorted(java.util.Comparator.comparingInt(String::length)).toList();
        for (String field : defaultFields) {
            if (!field.startsWith(prefix) || !table.containsKey(field)) continue;
            Object fieldValue = structurePath(value, field.substring(prefix.length()));
            if (fieldValue != NO_LOCAL_FIELD) setStructuredDefaultValue(field, fieldValue);
        }
        var bindings = sharedFields.entrySet().stream().filter(binding -> binding.getKey().startsWith(prefix))
                .sorted(java.util.Comparator.comparingInt(binding -> binding.getKey().length())).toList();
        for (var binding : bindings) {
            Object fieldValue = structurePath(value, binding.getKey().substring(prefix.length()));
            if (fieldValue == NO_LOCAL_FIELD) continue;
            if (instance == null) binding.getValue().setRawValue(binding.getKey(), fieldValue);
            else binding.getValue().setRawValue(binding.getKey(), fieldValue, instance);
        }
    }

    private static Object structurePath(Object value, String path) {
        Object current = value;
        for (String field : path.split("\\.")) {
            if (current instanceof MolangStruct structure && structure.contains(field)) {
                current = structure.get(field);
            } else if (current instanceof Map<?, ?> map) {
                var entry = map.entrySet().stream().filter(item -> item.getKey() instanceof String key
                        && key.equalsIgnoreCase(field)).findFirst();
                if (entry.isEmpty()) return NO_LOCAL_FIELD;
                current = entry.get().getValue();
            } else return NO_LOCAL_FIELD;
        }
        return current;
    }

    private void setStructuredDefaultValue(String name, @Nullable Object value) {
        Variable variable = table.get(name);
        if (value instanceof Number number) variable.set(number.doubleValue());
        else if (value instanceof String string) variable.set(string);
        else variable.setRaw(value instanceof MolangStruct structure ? structure.copy() : value);
    }

    private void setStructuredDefaultPath(String rootName, String name, @Nullable Object value) {
        if (rootName.equals(name)) {
            setStructuredDefaultValue(rootName, value);
            return;
        }
        Variable variable = table.get(rootName);
        MolangStruct structure = variable.structure();
        if (structure == null) {
            Object valueBeforeWrite = variable.value() == null ? variable.getRaw(null) : null;
            structure = valueBeforeWrite instanceof Map<?, ?> map ? MolangStruct.fromMap(map) : new MolangStruct();
            variable.setRaw(structure);
        }
        structure.putPath(name.substring(rootName.length() + 1), value);
    }

    private int localStructureRootEnd(String name) {
        int first = name.indexOf('.', name.indexOf('.') + 1);
        if (first < 0) return -1;
        if (table.containsKey(name.substring(0, first))) return first;
        int selected = first;
        for (int separator = name.indexOf('.', first + 1); separator >= 0;
             separator = name.indexOf('.', separator + 1)) {
            if (table.containsKey(name.substring(0, separator))) selected = separator;
        }
        return selected;
    }

    @ApiStatus.Internal
    public Object getLocalStructureField(String name, MolangInstance instance) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) {
            if (shared.hasStructureFields(name)) {
                MolangStruct structure = shared.getStructureSnapshot(name, instance);
                if (structure != null) return structure;
            }
            return shared.getRawValue(name, instance);
        }
        String structured = structuredDefaultSource(name);
        if (structured != null) {
            if (!beginResolve(structured)) return 0.0;
            try {
                Variable variable = table.get(structured);
                Object value = variable.value() == null ? variable.getRaw(instance) : variable.get(instance);
                if (structured.equals(name)) return value;
                Object field = structurePath(value, name.substring(structured.length() + 1));
                return field == NO_LOCAL_FIELD ? null : field;
            } finally {
                endResolve(structured);
            }
        }
        int namespaceEnd = name.indexOf('.');
        int rootEnd = name.indexOf('.', namespaceEnd + 1);
        if (name.startsWith("variable.") && rootEnd >= 0 && table.containsKey(name.substring(0, rootEnd))) {
            Object value = getLocalRawValue(name.substring(0, rootEnd), instance);
            for (String field : name.substring(rootEnd + 1).split("\\.")) {
                if (value instanceof MolangStruct structure) value = structure.get(field);
                else if (value instanceof Map<?, ?> values) value = MolangStruct.fromMap(values).get(field);
                else return null;
            }
            return value;
        }
        int separator = name.lastIndexOf('.');
        while (separator > namespaceEnd) {
            String prefix = name.substring(0, separator);
            Variable root = table.get(prefix);
            if (root != null) {
                Object value = root.structure();
                if (value instanceof MolangStruct) {
                    String[] fields = name.substring(separator + 1).split("\\.");
                    for (String field : fields) {
                        if (!(value instanceof MolangStruct structure) || !structure.contains(field)) return NO_LOCAL_FIELD;
                        value = structure.get(field);
                    }
                    return value;
                }
            }
            separator = name.lastIndexOf('.', separator - 1);
        }
        return NO_LOCAL_FIELD;
    }

    public void setValue(String name, Variable value) {
        VariableTable shared = sharedFieldSource(name);
        if (shared != null) {
            shared.setValue(name, value);
            return;
        }
        Variable variable = table.get(name);
        if (variable == null) {
            if (hasStructureRoot(name)) installStructuredDefault(name, value);
            else table.put(name, value);
        } else {
            variable.set(value);
        }
    }

    private boolean hasStructureRoot(String name) {
        if (!name.startsWith("variable.")) return false;
        int separator = name.indexOf('.', "variable.".length());
        if (separator < 0) return false;
        Variable root = getVariable(name.substring(0, separator));
        return root != null && (root.structure() != null || root.rawReference() instanceof Map<?, ?>);
    }

    private boolean shouldWriteStructure(String name) {
        if (!hasStructureRoot(name)) return false;
        int separator = name.indexOf('.', "variable.".length());
        return !table.containsKey(name) || table.containsKey(name.substring(0, separator));
    }

    public Variable computeIfAbsent(String name, Function<String, Variable> function) {
        Variable variable = table.get(name);
        if (variable == null) {
            if (parent == null) {
                variable = function.apply(name);
                table.put(name, variable);
                return variable;
            }
            return parent.computeIfAbsent(name, function);
        }
        return variable;
    }

    public void setParent(@Nullable VariableTable parent) {
        this.parent = parent;
        parentRevision++;
        if (initialValues != null) initialValues.clear();
    }

    public @Nullable VariableTable getParent() {
        return parent;
    }

    @ApiStatus.Internal
    public long parentRevision() {
        return parentRevision;
    }

    protected boolean beginResolve(String name) {
        return RESOLVING.get().computeIfAbsent(table, ignored -> new HashSet<>()).add(name);
    }

    protected void endResolve(String name) {
        Map<Map<String, Variable>, Set<String>> resolving = RESOLVING.get();
        Set<String> names = resolving.get(table);
        names.remove(name);
        if (names.isEmpty()) resolving.remove(table);
        if (resolving.isEmpty()) {
            RESOLVING.remove();
        }
    }
}
