package tsml.classifiers.distance_based.utils.classifier_building;

import com.google.common.collect.ImmutableSet;
import tsml.classifiers.EnhancedAbstractClassifier;
import tsml.classifiers.distance_based.elastic_ensemble.ElasticEnsemble;
import tsml.classifiers.distance_based.knn.KNN;
import tsml.classifiers.distance_based.knn.KNNLOOCV;
import weka.classifiers.Classifier;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ClassifierBuilderFactory<B extends Classifier> {

    public interface ClassifierBuilder<A extends Classifier> extends Supplier<A> {
        String toString();
        String getName();
        A build();

        @Override
        default A get() {
            return build();
        }
    }

    public static class SuppliedClassifierBuilder<A extends Classifier> implements ClassifierBuilder<A> {
        private final String name;
        private final Supplier<? extends A> supplier;

        public SuppliedClassifierBuilder(final String name, final Supplier<? extends A> supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        public String getName() {
            return name;
        }

        public Supplier<? extends A> getSupplier() {
            return supplier;
        }

        public A build() {
            A classifier = getSupplier().get();
            if(classifier instanceof EnhancedAbstractClassifier) {
                ((EnhancedAbstractClassifier) classifier).setClassifierName(getName());
            }
            return classifier;
        }

        @Override public String toString() {
            return name;
        }
    }

    private static ClassifierBuilderFactory<Classifier> INSTANCE;
    private final Map<String, ClassifierBuilder<? extends B>> classifierBuildersByName = new HashMap<>();
    private final Set<ClassifierBuilder<? extends B>> classifierBuilders = new HashSet<>();

    public ClassifierBuilderFactory() {}

    @Override public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getSimpleName()).append("{").append(System.lineSeparator());
        for(Map.Entry<String, ClassifierBuilder<? extends B>> entry : classifierBuildersByName.entrySet()) {
            stringBuilder.append("\t");
            stringBuilder.append(entry.getKey());
            stringBuilder.append(": ");
            stringBuilder.append(System.lineSeparator());
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public static ClassifierBuilderFactory<Classifier> getGlobalInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ClassifierBuilderFactory<>();
            INSTANCE.addAll(KNNLOOCV.FACTORY);
            INSTANCE.addAll(ElasticEnsemble.FACTORY);
        }
        INSTANCE.addAll(new CompileTimeClassifierBuilderFactory<KNN>());
        return INSTANCE;
    }

    public void addAll(ClassifierBuilderFactory<? extends B> other) {
        Set<? extends ClassifierBuilder<? extends B>> all = other.all();
        addAll(all);
    }

    public Set<ClassifierBuilder<? extends B>> all() {
        return new HashSet<>(classifierBuilders);
    }

    public void addAll(Iterable<? extends ClassifierBuilder<? extends B>> all) {
        for(ClassifierBuilder<? extends B> builder : all) {
            add(builder);
        }
    }

    public void addAll(ClassifierBuilder<? extends B>... builders) {
        addAll(Arrays.asList(builders));
    }

    public void addAll(Supplier<? extends ClassifierBuilder<? extends B>>... suppliers) {
        addAll(Arrays.stream(suppliers).map(Supplier::get).collect(Collectors.toList()));
    }

    public <C extends ClassifierBuilder<? extends B>> C add(C classifierBuilder) {
        String name = classifierBuilder.getName();
        name = name.toLowerCase();
        if(classifierBuildersByName.containsKey(name)) {
            throw new IllegalArgumentException("oops, a classifier already exists under the name: " + name);
        } else if(classifierBuilders.contains(classifierBuilder)) {
            throw new IllegalArgumentException("oops, a classifier already exists under that supplier.");
        } else {
            classifierBuilders.add(classifierBuilder);
            classifierBuildersByName.put(name, classifierBuilder);
        }
        return classifierBuilder;
    }

    public ClassifierBuilder<? extends B> getClassifierBuilderByName(String name) {
        name = name.toLowerCase();
        ClassifierBuilder<? extends B> classifierBuilder = classifierBuildersByName.get(name);
        return classifierBuilder;
    }

    public Set<ClassifierBuilder<? extends B>> getClassifierBuildersByNames(String... names) {
        return getClassifierBuildersByNames(Arrays.asList(names));
    }

    public Set<ClassifierBuilder<? extends B>> getClassifierBuildersByNames(Iterable<String> names) {
        Set<ClassifierBuilder<? extends B>> set = new HashSet<>();
        for(String name : names) {
            set.add(getClassifierBuilderByName(name));
        }
        return ImmutableSet.copyOf(set);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(getGlobalInstance().toString());
    }
}
