/*
 * Copyright (c) 2018-2024. Robin Hillyard
 */

package com.phasmidsoftware.dsaipg.util;

import com.phasmidsoftware.dsaipg.adt.pq.PriorityQueue;
import com.phasmidsoftware.dsaipg.adt.pq.FourAryHeap;
import com.phasmidsoftware.dsaipg.adt.pq.FibonacciHeap;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import com.phasmidsoftware.dsaipg.adt.pq.PQException;



/**
 * This class implements a simple Benchmark utility for measuring the running time of algorithms.
 * It is part of the repository for the INFO6205 class, taught by Prof. Robin Hillyard.
 *
 * <p>
 * In general, the benchmark class handles three phases of a "run:"
 * <ol>
 *     <li>The pre-function which prepares the input to the study function (field fPre) (may be null);</li>
 *     <li>The study function itself (field fRun) -- assumed to be a mutating function since it does not return a result;</li>
 *     <li>The post-function which cleans up and/or checks the results of the study function (field fPost) (may be null).</li>
 * </ol>
 * </p>
 *
 * @param <T> The generic type T is that of the input to the function f which you will pass into the constructor.
 */
public class Benchmark_Timer<T> implements Benchmark<T> {

    /**
     * Calculate the appropriate number of warmup runs.
     *
     * @param m the number of runs.
     * @return at least one and at most the lower of four or m/15.
     */
    static int getWarmupRuns(int m) {
        return Integer.max(1, Integer.min(3, m / 15));
    }

    /**
     * Run function f m times and return the average time in milliseconds.
     *
     * @param supplier a Supplier of a T
     * @param m        the number of times the function f will be called.
     * @return the average number of milliseconds taken for each run of function f.
     */
    public double runFromSupplier(Supplier<T> supplier, int m) {
        final Function<T, T> function = t -> {
            fRun.accept(t);
            return t;
        };
        // Warmup phase
        new Timer().repeat(getWarmupRuns(m), true, supplier, function, fPre, null);

        // Timed phase
        return new Timer().repeat(m, false, supplier, function, fPre, fPost);
    }

    /**
     * Constructor for a Benchmark_Timer with the option of specifying all three functions.
     */
    public Benchmark_Timer(String description, UnaryOperator<T> fPre, Consumer<T> fRun, Consumer<T> fPost) {
        this.description = description;
        this.fPre = fPre;
        this.fRun = fRun;
        this.fPost = fPost;
    }

    /**
     * Constructor for a Benchmark_Timer with the option of specifying a pre-function and run function.
     */
    public Benchmark_Timer(String description, UnaryOperator<T> fPre, Consumer<T> fRun) {
        this(description, fPre, fRun, null);
    }

    /**
     * Constructor for a Benchmark_Timer with only fRun and fPost Consumer parameters.
     */
    public Benchmark_Timer(String description, Consumer<T> fRun, Consumer<T> fPost) {
        this(description, null, fRun, fPost);
    }

    /**
     * Constructor for a Benchmark_Timer where only the (timed) run function is specified.
     */
    public Benchmark_Timer(String description, Consumer<T> f) {
        this(description, null, f, null);
    }

    private final String description;
    private final UnaryOperator<T> fPre;
    private final Consumer<T> fRun;
    private final Consumer<T> fPost;

    private static final Random random = new Random();
    private static final int M = 4095; // Max heap size
    private static final int[] INPUT_SIZES = {1000, 2000, 4000, 8000, 16000};

    private static final Map<String, List<Double>> insertionTimesMap = new HashMap<>();
    private static final Map<String, List<Double>> removalTimesMap = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("\n--- Heap Benchmarking ---");

        List<Supplier<Object>> heapSuppliers = Arrays.asList(
                () -> new PriorityQueue<Integer>(M, true, Comparator.naturalOrder(), false),
                () -> new PriorityQueue<Integer>(M, true, Comparator.naturalOrder(), true),
                () -> new FourAryHeap<Integer>(M, Comparator.naturalOrder(), false, false),
                () -> new FourAryHeap<Integer>(M, Comparator.naturalOrder(), true, true),
                FibonacciHeap::new
        );

        List<String> heapNames = Arrays.asList(
                "BinaryHeap", "BinaryHeapFloyd", "4AryHeap", "4AryHeapFloyd", "FibonacciHeap"
        );

        for (int i = 0; i < heapSuppliers.size(); i++) {
            String heapName = heapNames.get(i);
            Supplier<Object> heapSupplier = heapSuppliers.get(i);

            List<Double> insertionTimes = new ArrayList<>();
            List<Double> removalTimes = new ArrayList<>();

            for (int size : INPUT_SIZES) {
                System.out.println("\nHeap: " + heapName + " | Input Size: " + size);

                // Measure Insertion Time
                Benchmark_Timer<Object> insertionBenchmark = new Benchmark_Timer<>(
                        heapName + " Insertions",
                        heap -> benchmarkHeap(heapName, heapSupplier, size, 0)
                );
                double insertionTime = insertionBenchmark.runFromSupplier(heapSupplier, 10);
                insertionTimes.add(insertionTime);

                // Measure Removal Time
                Benchmark_Timer<Object> removalBenchmark = new Benchmark_Timer<>(
                        heapName + " Removals",
                        heap -> benchmarkHeap(heapName, heapSupplier, 0, size / 4) // 25% removals
                );
                double removalTime = removalBenchmark.runFromSupplier(heapSupplier, 10);
                removalTimes.add(removalTime);

                System.out.printf("Input Size: %d | Insertion Time: %.6f ms | Removal Time: %.6f ms%n",
                        size, insertionTime, removalTime);
            }

            insertionTimesMap.put(heapName, insertionTimes);
            removalTimesMap.put(heapName, removalTimes);
        }

        exportToCSV();
    }

    static void benchmarkHeap(String description, Supplier<Object> heapSupplier, int inserts, int removes) {
        Object heapInstance = heapSupplier.get();
        int nextPrint = 1000;

        if (heapInstance instanceof PriorityQueue) {
            PriorityQueue<Integer> pq = (PriorityQueue<Integer>) heapInstance;

            for (int i = 1; i <= inserts; i++) {
                pq.give(random.nextInt());
                if (i == nextPrint || i == inserts) {
                    System.out.println(description + " - Insertions completed: " + i);
                    nextPrint *= 2;
                }
            }

            for (int i = 1; i <= removes; i++) {
                if (!pq.isEmpty()) {
                    try {
                        pq.take();
                    } catch (PQException e) {
                        System.err.println("PriorityQueue error during removal: " + e.getMessage());
                    }
                }
            }
        } else if (heapInstance instanceof FourAryHeap) {
            FourAryHeap<Integer> faHeap = (FourAryHeap<Integer>) heapInstance;

            for (int i = 1; i <= inserts; i++) {
                faHeap.insert(random.nextInt());
                if (i == nextPrint || i == inserts) {
                    System.out.println(description + " - Insertions completed: " + i);
                    nextPrint *= 2;
                }
            }

            for (int i = 1; i <= removes; i++)
                if (faHeap.size() > 0) faHeap.removeTop();

        } else if (heapInstance instanceof FibonacciHeap) {
            FibonacciHeap<Integer> fibHeap = (FibonacciHeap<Integer>) heapInstance;

            for (int i = 1; i <= inserts; i++) {
                fibHeap.insert(random.nextInt());
                if (i == nextPrint || i == inserts) {
                    System.out.println(description + " - Insertions completed: " + i);
                    nextPrint *= 2;
                }
            }

            for (int i = 1; i <= removes; i++)
                if (fibHeap.size() > 0) fibHeap.removeMin();
        }
    }

    private static void exportToCSV() {
        try (PrintWriter writer = new PrintWriter(new File("loglog_benchmark_results.csv"))) {
            writer.print("InputSize");

            for (String heapName : insertionTimesMap.keySet()) {
                writer.print("," + heapName + "_Insertion");
                writer.print("," + heapName + "_Removal");
            }
            writer.println();

            for (int i = 0; i < INPUT_SIZES.length; i++) {
                writer.print(INPUT_SIZES[i]);

                for (String heapName : insertionTimesMap.keySet()) {
                    writer.printf(",%.6f,%.6f",
                            insertionTimesMap.get(heapName).get(i),
                            removalTimesMap.get(heapName).get(i));
                }
                writer.println();
            }

            System.out.println("CSV Exported Successfully: loglog_benchmark_results.csv");

        } catch (FileNotFoundException e) {
            System.err.println("Error writing CSV: " + e.getMessage());
        }
    }
}