package com.onlineinteract.ga.base;

import com.onlineinteract.adt.TokenData;
import com.onlineinteract.utility.TokenDataFetcher;
import org.jetbrains.annotations.NotNull;
import org.uncommons.maths.binary.BitString;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.*;
import org.uncommons.watchmaker.framework.operators.BitStringCrossover;
import org.uncommons.watchmaker.framework.operators.BitStringMutation;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.selection.RouletteWheelSelection;
import org.uncommons.watchmaker.framework.termination.GenerationCount;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AlgoV6: simple 3-parameter trend strategy.
 *
 * Strategy: Fast/Slow SMA crossover with a minimum separation filter.
 *   fast = SMA(fastPeriod)
 *   slow = SMA(slowPeriod)
 *   sepPct = 100 * (fast/slow - 1)
 *
 *   BUY  when fast crosses above slow AND sepPct >= minSepPct
 *   SELL when fast crosses below slow
 *
 * Searchable params:
 *   - fastPeriod (int)
 *   - slowPeriod (int)
 *   - minSepPct (double, %)
 */
public class AlgoV6 {
    static final double INITIAL_INVESTMENT = 100;
    static final int BUY = 1;
    static final int SELL = -1;

    static String TOKEN;
    static String WORKING_DIRECTORY;
    static int DATA_RESOLUTION;
    static double FEE_RATE;
    static String FROM_DATE;
    static String TO_DATE;
    static TokenData ORIGINAL_DATA;
    static TokenData TOKEN_DATA;

    static public enum FitnessMethod {X_OVER_HODL, PROFIT, COMPOUND_RETURN_RATE}

    static FitnessMethod FITNESS_METHOD;
    static boolean GA_SEED;

    // Begin Agentic Workflow Section - 0. fixed variables
    // No fixed strategy params for this simplified concept.
    // End Agentic Workflow Section - 0. fixed variables

    // Begin Agentic Workflow Section - 1. input parameters
    static int fastPeriod;
    static int slowPeriod;
    static double minSepPct;
    // End Agentic Workflow Section - 1. input parameters

    /**
     * GA params.
     */
    static final int POPULATION_SIZE = 2000;
    static final int MAX_GENERATIONS = 800;
    static final int RUN_SIZE = 100;
    static final double CROSSOVER_PROBABILITY = 0.9;
    static final double MUTATION_PROBABILITY = 0.2;
    static final int ELITISM_COUNT = (int) Math.max(1, Math.round(POPULATION_SIZE * 0.02));

    /**
     * rawMinSep(0..1023)/INDICATOR_DIVIDER => minSepPct (percent)
     */
    static int INDICATOR_DIVIDER = 100;

    // Begin Agentic Workflow Section - 2. bit positions
    static BitPositions bitPositions = BitPositions.assemble(
            // 7 bits -> 0..127
            "FAST_PERIOD", 7,
            // 8 bits -> 0..255
            "SLOW_PERIOD", 8,
            // 10 bits -> 0..1023 => 0.00%..10.23%
            "MIN_SEP_PCT", 10
    );
    // End Agentic Workflow Section - 2. bit positions

    public static void main(String[] args) throws IOException {
        String choice = choicePrompt(args);

        if ("1".equals(choice)
                || "SCE".equalsIgnoreCase(choice)
                || (args.length > 0 && "SCE".equalsIgnoreCase(args[0]))) {
            singleCandidateEvaluation();
        } else if ("2".equals(choice)
                || "GA".equalsIgnoreCase(choice)
                || (args.length > 0 && "GA".equalsIgnoreCase(args[0]))) {
            geneticAlgorithmSearch();
        } else {
            System.out.println("Invalid choice. Please enter 1 (SCE) or 2 (GA).");
        }
    }

    // Begin Agentic Workflow Section - 3. genetic algorithm search
    private static void geneticAlgorithmSearch() throws IOException {
        TOKEN = "BTC";
//        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\kraken";
        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\binance";
        DATA_RESOLUTION = 15;
        FEE_RATE = 0.001;
        FROM_DATE = "2020-07-24";
        TO_DATE = "2025-07-30";

        // Seed params
        fastPeriod = 20;
        slowPeriod = 80;
        minSepPct = 0.3;

        GA_SEED = false;
        FITNESS_METHOD = FitnessMethod.X_OVER_HODL;

        INDICATOR_DIVIDER = 100;
        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);

        CandidateFactory<BitString> candidateFactory = new BitStringFactory(bitPositions.getBitWidth());
        EvolutionaryOperator<BitString> pipeline = new EvolutionPipeline<>(Arrays.asList(
                new ProbabilisticCrossover(1, new Probability(CROSSOVER_PROBABILITY)),
                new BitStringMutation(new Probability(MUTATION_PROBABILITY))
        ));

        FitnessEvaluator<BitString> fitnessEvaluator = new FitnessFunction();
        ProgressListener progressListener = new ProgressListener();
        EvolutionEngine<BitString> engine = new GenerationalEvolutionEngine<>(
                candidateFactory,
                pipeline,
                fitnessEvaluator,
                new RouletteWheelSelection(),
                new Random()
        );

        engine.addEvolutionObserver(progressListener);
        StagnationTermination stagnationTermination = new StagnationTermination(RUN_SIZE);

        List<BitString> seedCandidates = generateSeedCandidates();

        long startTime = System.currentTimeMillis();
        System.out.println("GA start...");
        if (GA_SEED)
            engine.evolve(POPULATION_SIZE, ELITISM_COUNT, seedCandidates, new GenerationCount(MAX_GENERATIONS), stagnationTermination);
        else
            engine.evolve(POPULATION_SIZE, ELITISM_COUNT, new GenerationCount(MAX_GENERATIONS), stagnationTermination);
        System.out.println("GA end...\n");
        long endTime = System.currentTimeMillis();

        BitString bestSolution = progressListener.getBestCandidate();

        fastPeriod = bitsToInt(bestSolution,
                bitPositions.get("FAST_PERIOD").getStartPos(),
                bitPositions.get("FAST_PERIOD").getEndPos());

        slowPeriod = bitsToInt(bestSolution,
                bitPositions.get("SLOW_PERIOD").getStartPos(),
                bitPositions.get("SLOW_PERIOD").getEndPos());

        int rawMinSep = bitsToInt(bestSolution,
                bitPositions.get("MIN_SEP_PCT").getStartPos(),
                bitPositions.get("MIN_SEP_PCT").getEndPos());
        minSepPct = rawMinSep / (double) INDICATOR_DIVIDER;

        System.out.printf("Best solution (fastPeriod, slowPeriod, minSepPct%%): %d, %d, %.2f%%%n",
                fastPeriod, slowPeriod, minSepPct);
        System.out.printf("GA execution time: %d milliseconds%n", (endTime - startTime));

        double profit = executeStandardTradeRules("GA");
        int fromPos = Math.max(fastPeriod, slowPeriod);
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos, toPos);
        double xOverHodl = profit / hodlProfit;
        System.out.println("xOverHodl: " + xOverHodl + " - profit: " + profit + " - hodlProfit: " + hodlProfit);
    }
    // End Agentic Workflow Section - 3. genetic algorithm search

    // Begin Agentic Workflow Section - 4. single candidate evaluation
    private static void singleCandidateEvaluation() throws IOException {
        TOKEN = "BTC";
//        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\kraken";
        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\binance";
        DATA_RESOLUTION = 15;
        FEE_RATE = 0.001;
        FROM_DATE = "2020-07-24";
        TO_DATE = "2025-07-30";

        fastPeriod = 20;
        slowPeriod = 80;
        minSepPct = 0.3;

        System.out.println("Begin Data Fetch: " + FROM_DATE);
        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        System.out.println("End Data Fetch: " + TO_DATE);
        System.out.println(ORIGINAL_DATA.getTokenSymbol());

        double profit = executeStandardTradeRules("SCE");
        int fromPos = Math.max(fastPeriod, slowPeriod);
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos, toPos);
        double xOverHodl = profit / hodlProfit;
        System.out.println("xOverHodl: " + xOverHodl + " - profit: " + profit + " - hodlProfit: " + hodlProfit);
    }
    // End Agentic Workflow Section - 4. single candidate evaluation

    private static @NotNull String choicePrompt(String[] args) {
        String choice = "";
        if (args.length == 0) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Choose mode [1 = SCE (Single Candidate Evaluation), 2 = GA (Genetic Algorithm Search)]: ");
            choice = scanner.nextLine().trim();
        }
        return choice;
    }

    // Begin Agentic Workflow Section - 5. execute standard trading rules
    public static double executeStandardTradeRules(String type) {
        System.out.println("Generating signals");
        System.out.println("1: " + LocalDateTime.now());
        TokenData tokenData = generateCustomSignals(fastPeriod, slowPeriod, minSepPct);
        System.out.println("2: " + LocalDateTime.now());
        System.out.println("Custom signals complete\n");

        System.out.println("Calculating profit");
        System.out.println("3: " + LocalDateTime.now());
        int fromPos = Math.max(fastPeriod, slowPeriod);
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double profit = calculateProfit(fromPos, toPos, tokenData);
        System.out.println("4: " + LocalDateTime.now());
        System.out.println("Calculating profit complete");

        System.out.printf("\nInitial Investment in USD $: %.2f%n", INITIAL_INVESTMENT);
        System.out.printf("=> Profit in USD $: %.2f%n", profit);
        System.out.printf("=> Current Holdings in USD $: %.2f%n%n", profit + INITIAL_INVESTMENT);

        if (type.equals("SCE")) {
            TOKEN_DATA = tokenData;
            double crr = calculateCompoundedReturnRate(fromPos, toPos, tokenData);
            System.out.println("Compounded Return Rate: " + Math.max(crr, 0));
        }

        return profit;
    }
    // End Agentic Workflow Section - 5. execute standard trading rules

    // Begin Agentic Workflow Section - 6. generate custom signals
    public static TokenData generateCustomSignals(int fastPeriod, int slowPeriod, double minSepPct) {
        TokenData tokenData = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        double[] close = ORIGINAL_DATA.getClose();
        int n = close.length;

        double[] fast = new double[n];
        double[] slow = new double[n];
        double[] sep = new double[n];
        double[] orderSignal = new double[n];
        Arrays.fill(fast, 0.0);
        Arrays.fill(slow, 0.0);
        Arrays.fill(sep, 0.0);
        Arrays.fill(orderSignal, 0.0);

        // SMA fast
        double sumFast = 0.0;
        for (int i = 0; i < n; i++) {
            sumFast += close[i];
            if (i >= fastPeriod) sumFast -= close[i - fastPeriod];
            if (i >= fastPeriod - 1) fast[i] = sumFast / fastPeriod;
        }

        // SMA slow
        double sumSlow = 0.0;
        for (int i = 0; i < n; i++) {
            sumSlow += close[i];
            if (i >= slowPeriod) sumSlow -= close[i - slowPeriod];
            if (i >= slowPeriod - 1) slow[i] = sumSlow / slowPeriod;
        }

        int start = Math.max(Math.max(fastPeriod, slowPeriod), 2);

        // separation
        for (int i = start; i < n; i++) {
            if (slow[i] != 0.0) {
                sep[i] = 100.0 * (fast[i] / slow[i] - 1.0);
            }
        }

        for (int i = start; i < n; i++) {
            if (fast[i - 1] == 0.0 || slow[i - 1] == 0.0 || fast[i] == 0.0 || slow[i] == 0.0) continue;

            boolean crossUp = fast[i - 1] <= slow[i - 1] && fast[i] > slow[i];
            boolean crossDown = fast[i - 1] >= slow[i - 1] && fast[i] < slow[i];

            if (crossUp && sep[i] >= minSepPct) {
                orderSignal[i] = BUY;
            } else if (crossDown) {
                orderSignal[i] = SELL;
            }
        }

        tokenData.getData().put("Fast SMA", fast);
        tokenData.getData().put("Slow SMA", slow);
        tokenData.getData().put("SepPct", sep);
        tokenData.getData().put("Order Signal", orderSignal);
        return tokenData;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    // Begin Agentic Workflow Section - 7. calculate profit
    public static double calculateProfit(int fromPos, int toPos, TokenData tokenData) {
        double[] holdingsVector = new double[ORIGINAL_DATA.getClose().length];
        Arrays.fill(holdingsVector, 0.0);

        double currentHoldings = INITIAL_INVESTMENT;
        boolean inPosition = false;
        double tokenAmount = 0.0;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = ORIGINAL_DATA.getClose();

        for (int i = Math.max(0, fromPos); i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) continue;

            if (orderSignal[i] == BUY && !inPosition) {
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;

                tokenAmount = currentHoldings / closePrice[i];
                inPosition = true;
                holdingsVector[i] = currentHoldings;

            } else if (orderSignal[i] == SELL && inPosition) {
                double sellValue = tokenAmount * closePrice[i];
                double fee = FEE_RATE * sellValue;
                sellValue -= fee;

                currentHoldings = sellValue;
                inPosition = false;
                tokenAmount = 0.0;
                holdingsVector[i] = currentHoldings;

            } else {
                holdingsVector[i] = inPosition ? tokenAmount * closePrice[i] : currentHoldings;
            }
        }

        tokenData.getData().put("Holdings", holdingsVector);

        double finalHoldings = inPosition ? tokenAmount * closePrice[toPos] : currentHoldings;
        return finalHoldings - INITIAL_INVESTMENT;
    }
    // End Agentic Workflow Section - 7. calculate profit

    public static double calculateCompoundedReturnRate(int fromPos, int toPos, TokenData tokenData) {
        double compoundedReturnRate = 0.0;
        boolean inPosition = false;
        double currentHoldings = INITIAL_INVESTMENT;
        double tokenAmount = 0.0;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = ORIGINAL_DATA.getClose();

        for (int i = fromPos; i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) continue;

            if (orderSignal[i] == BUY && !inPosition) {
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;
                tokenAmount = currentHoldings / closePrice[i];
                inPosition = true;

            } else if (orderSignal[i] == SELL && inPosition) {
                double sellValue = tokenAmount * closePrice[i];
                double fee = FEE_RATE * sellValue;
                sellValue -= fee;

                double logReturn = Math.log(sellValue / currentHoldings);
                compoundedReturnRate += logReturn * 100.0;

                currentHoldings = sellValue;
                tokenAmount = 0.0;
                inPosition = false;
            }
        }

        return compoundedReturnRate;
    }

    public static double executeStandardModel(int fromPos, int toPos) {
        double beginPrice = ORIGINAL_DATA.getClose()[fromPos];
        double endPrice = ORIGINAL_DATA.getClose()[toPos];

        double differencePrice = endPrice - beginPrice;
        double percentageDifference = differencePrice / beginPrice;

        double profit = INITIAL_INVESTMENT * percentageDifference;
        double currentHoldings = profit + INITIAL_INVESTMENT;

        double fee = FEE_RATE * currentHoldings;
        currentHoldings -= fee;

        return profit;
    }

    private static int bitsToInt(BitString bitString, int start, int end) {
        int value = 0;
        for (int i = start; i < end; i++) {
            if (bitString.getBit(i)) {
                value |= 1 << (i - start);
            }
        }
        return value;
    }

    // Begin Agentic Workflow Section - 8. generate seed candidates
    private static List<BitString> generateSeedCandidates() {
        BitString seed = new BitString(bitPositions.getBitWidth());

        intToBits(seed,
                bitPositions.get("FAST_PERIOD").getStartPos(),
                bitPositions.get("FAST_PERIOD").getEndPos(),
                fastPeriod);

        intToBits(seed,
                bitPositions.get("SLOW_PERIOD").getStartPos(),
                bitPositions.get("SLOW_PERIOD").getEndPos(),
                slowPeriod);

        int rawMinSep = (int) Math.round(minSepPct * INDICATOR_DIVIDER);
        intToBits(seed,
                bitPositions.get("MIN_SEP_PCT").getStartPos(),
                bitPositions.get("MIN_SEP_PCT").getEndPos(),
                rawMinSep);

        System.out.println("SEED DEBUG:");
        System.out.printf("Input fastPeriod = %d%n", fastPeriod);
        System.out.printf("Input slowPeriod = %d%n", slowPeriod);
        System.out.printf("Input minSepPct = %.3f%%, raw = %d, decoded = %.3f%%%n",
                minSepPct, rawMinSep, rawMinSep / (double) INDICATOR_DIVIDER);

        return Collections.singletonList(seed);
    }
    // End Agentic Workflow Section - 8. generate seed candidates

    private static void intToBits(BitString bitString, int start, int end, int value) {
        for (int i = start; i < end; i++) {
            int bitIndex = i - start;
            boolean bit = ((value >> bitIndex) & 1) == 1;
            bitString.setBit(i, bit);
        }
    }

    // Begin Agentic Workflow Section - 9. fitness function
    private static class FitnessFunction implements FitnessEvaluator<BitString> {
        @Override
        public double getFitness(BitString candidate, List<? extends BitString> population) {
            int fast = bitsToInt(candidate,
                    bitPositions.get("FAST_PERIOD").getStartPos(),
                    bitPositions.get("FAST_PERIOD").getEndPos());

            int slow = bitsToInt(candidate,
                    bitPositions.get("SLOW_PERIOD").getStartPos(),
                    bitPositions.get("SLOW_PERIOD").getEndPos());

            int rawMinSep = bitsToInt(candidate,
                    bitPositions.get("MIN_SEP_PCT").getStartPos(),
                    bitPositions.get("MIN_SEP_PCT").getEndPos());
            double minSep = rawMinSep / (double) INDICATOR_DIVIDER;

            int fromPos = Math.max(fast, slow);

            // Guards: ensure meaningful windows and ordering.
            if (fast < 2 || slow < 3 || fast >= slow || fromPos >= ORIGINAL_DATA.getClose().length) {
                return 0;
            }

            TokenData tokenData = generateCustomSignals(fast, slow, minSep);
            int toPos = ORIGINAL_DATA.getClose().length - 1;

            double profit = calculateProfit(fromPos, toPos, tokenData);
            double hodlProfit = executeStandardModel(fromPos, toPos);

            double fitnessScore = 0;
            if (FITNESS_METHOD == FitnessMethod.X_OVER_HODL)
                fitnessScore = profit / hodlProfit;
            if (FITNESS_METHOD == FitnessMethod.PROFIT)
                fitnessScore = profit;
            if (FITNESS_METHOD == FitnessMethod.COMPOUND_RETURN_RATE)
                fitnessScore = calculateCompoundedReturnRate(fromPos, toPos, tokenData);

            return Math.max(fitnessScore, 0);
        }

        @Override
        public boolean isNatural() {
            return true;
        }
    }
    // End Agentic Workflow Section - 9. fitness function

    private static class ProbabilisticCrossover implements EvolutionaryOperator<BitString> {
        private final BitStringCrossover crossoverOperator;
        private final Probability probability;

        public ProbabilisticCrossover(int crossoverPoints, Probability probability) {
            this.crossoverOperator = new BitStringCrossover(crossoverPoints);
            this.probability = probability;
        }

        @Override
        public List<BitString> apply(List<BitString> population, Random rng) {
            if (rng.nextDouble() < probability.doubleValue()) {
                return crossoverOperator.apply(population, rng);
            }
            return population;
        }
    }

    private static class StagnationTermination implements TerminationCondition {
        private final int maxGenerationsWithoutImprovement;
        private double lastBestFitness = Double.NEGATIVE_INFINITY;
        private int stagnantGenerations = 0;

        public StagnationTermination(int maxGenerationsWithoutImprovement) {
            this.maxGenerationsWithoutImprovement = maxGenerationsWithoutImprovement;
        }

        @Override
        public boolean shouldTerminate(PopulationData<?> populationData) {
            double currentBestFitness = populationData.getBestCandidateFitness();
            if (currentBestFitness > lastBestFitness) {
                lastBestFitness = currentBestFitness;
                stagnantGenerations = 0;
            } else {
                stagnantGenerations++;
            }
            return stagnantGenerations >= maxGenerationsWithoutImprovement;
        }
    }

    // Begin Agentic Workflow Section - 10. progress listener
    private static class ProgressListener implements EvolutionObserver<BitString> {
        private BitString bestCandidate;

        @Override
        public void populationUpdate(PopulationData<? extends BitString> data) {
            bestCandidate = data.getBestCandidate();

            int fast = bitsToInt(bestCandidate,
                    bitPositions.get("FAST_PERIOD").getStartPos(),
                    bitPositions.get("FAST_PERIOD").getEndPos());

            int slow = bitsToInt(bestCandidate,
                    bitPositions.get("SLOW_PERIOD").getStartPos(),
                    bitPositions.get("SLOW_PERIOD").getEndPos());

            int rawMinSep = bitsToInt(bestCandidate,
                    bitPositions.get("MIN_SEP_PCT").getStartPos(),
                    bitPositions.get("MIN_SEP_PCT").getEndPos());
            double minSep = rawMinSep / (double) INDICATOR_DIVIDER;

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            String formattedDate = now.format(formatter);

            System.out.printf(
                    "Generation %d: Best fitness: %.3f, Solution (fast, slow, minSep%%): %d, %d, %.2f%% - %s%n",
                    data.getGenerationNumber(),
                    data.getBestCandidateFitness(),
                    fast,
                    slow,
                    minSep,
                    formattedDate
            );
        }

        public BitString getBestCandidate() {
            return bestCandidate;
        }
    }
    // End Agentic Workflow Section - 10. progress listener

    private static class BitStringFactory implements CandidateFactory<BitString> {
        private final int length;

        public BitStringFactory(int length) {
            this.length = length;
        }

        @Override
        public BitString generateRandomCandidate(Random rng) {
            return new BitString(length, rng);
        }

        @Override
        public List<BitString> generateInitialPopulation(int populationSize, Random rng) {
            List<BitString> population = new ArrayList<>(populationSize);
            for (int i = 0; i < populationSize; i++) {
                population.add(generateRandomCandidate(rng));
            }
            return population;
        }

        @Override
        public List<BitString> generateInitialPopulation(int populationSize, Collection<BitString> seedCandidates, Random rng) {
            List<BitString> population = new ArrayList<>(populationSize);
            population.addAll(seedCandidates);
            for (int i = seedCandidates.size(); i < populationSize; i++) {
                population.add(generateRandomCandidate(rng));
            }
            return population;
        }
    }

    public static final class BitPositions {

        public static final class BitField {
            private final int length;
            private final int startPos;
            private final int endPos;

            public BitField(int length, int startPos) {
                this.length = length;
                this.startPos = startPos;
                this.endPos = startPos + length;
            }

            public int getLength() {
                return length;
            }

            public int getStartPos() {
                return startPos;
            }

            public int getEndPos() {
                return endPos;
            }
        }

        private final LinkedHashMap<String, BitField> fields;
        private final int bitWidth;

        private BitPositions(LinkedHashMap<String, BitField> fields, int bitWidth) {
            this.fields = fields;
            this.bitWidth = bitWidth;
        }

        public static BitPositions assemble(Object... kv) {
            if (kv.length % 2 != 0) {
                throw new IllegalArgumentException("Must pass even number of arguments: key, value, key, value...");
            }

            LinkedHashMap<String, BitField> m = new LinkedHashMap<>();
            int cursor = 0;

            for (int i = 0; i < kv.length; i += 2) {
                Object k = kv[i];
                Object v = kv[i + 1];

                if (!(k instanceof String)) {
                    throw new IllegalArgumentException("Key at index " + i + " is not a String: " + k);
                }
                if (!(v instanceof Integer)) {
                    throw new IllegalArgumentException("Value at index " + (i + 1) + " is not an Integer: " + v);
                }

                String key = (String) k;
                int length = (Integer) v;

                if (length < 0) {
                    throw new IllegalArgumentException("Length for key '" + key + "' must be >= 0, got: " + length);
                }
                if (m.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate key: " + key);
                }

                BitField field = new BitField(length, cursor);
                m.put(key, field);
                cursor = field.getEndPos();
            }

            return new BitPositions(m, cursor);
        }

        public BitField get(String key) {
            return fields.get(key);
        }

        public int getBitWidth() {
            return bitWidth;
        }
    }
}
