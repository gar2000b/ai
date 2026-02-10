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
 * AlgoV10: simple 3-parameter MACD-like EMA crossover.
 *
 * Strategy:
 *   - Compute EMA(fast), EMA(slow)
 *   - MACD = EMA(fast) - EMA(slow)
 *   - Signal = EMA(signalPeriod) of MACD
 *   - BUY  when MACD crosses above Signal
 *   - SELL when MACD crosses below Signal
 *
 * Searchable params:
 *   - fastPeriod (int)
 *   - slowPeriod (int)
 *   - signalPeriod (int)
 */
public class AlgoV10 {
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
    // No fixed strategy params.
    // End Agentic Workflow Section - 0. fixed variables

    // Begin Agentic Workflow Section - 1. input parameters
    static int fastPeriod;
    static int slowPeriod;
    static int signalPeriod;
    // End Agentic Workflow Section - 1. input parameters

    static final int POPULATION_SIZE = 2000;
    static final int MAX_GENERATIONS = 800;
    static final int RUN_SIZE = 100;
    static final double CROSSOVER_PROBABILITY = 0.9;
    static final double MUTATION_PROBABILITY = 0.2;
    static final int ELITISM_COUNT = (int) Math.max(1, Math.round(POPULATION_SIZE * 0.02));

    static int INDICATOR_DIVIDER = 1;

    // Begin Agentic Workflow Section - 2. bit positions
    static BitPositions bitPositions = BitPositions.assemble(
            // 7 bits -> 0..127
            "FAST", 7,
            // 8 bits -> 0..255
            "SLOW", 8,
            // 7 bits -> 0..127
            "SIGNAL", 7
    );
    // End Agentic Workflow Section - 2. bit positions

    public static void main(String[] args) throws IOException {
        String choice = choicePrompt(args);
        if ("1".equals(choice) || "SCE".equalsIgnoreCase(choice) || (args.length > 0 && "SCE".equalsIgnoreCase(args[0]))) {
            singleCandidateEvaluation();
        } else if ("2".equals(choice) || "GA".equalsIgnoreCase(choice) || (args.length > 0 && "GA".equalsIgnoreCase(args[0]))) {
            geneticAlgorithmSearch();
        } else {
            System.out.println("Invalid choice. Please enter 1 (SCE) or 2 (GA).");
        }
    }

    // Begin Agentic Workflow Section - 3. genetic algorithm search
    private static void geneticAlgorithmSearch() throws IOException {
        TOKEN = "BTC";
        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\binance";
        DATA_RESOLUTION = 15;
        FEE_RATE = 0.001;
        FROM_DATE = "2020-07-24";
        TO_DATE = "2025-07-30";

        fastPeriod = 12;
        slowPeriod = 26;
        signalPeriod = 9;

        GA_SEED = false;
        FITNESS_METHOD = FitnessMethod.X_OVER_HODL;

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

        BitString best = progressListener.getBestCandidate();

        fastPeriod = bitsToInt(best, bitPositions.get("FAST").getStartPos(), bitPositions.get("FAST").getEndPos());
        slowPeriod = bitsToInt(best, bitPositions.get("SLOW").getStartPos(), bitPositions.get("SLOW").getEndPos());
        signalPeriod = bitsToInt(best, bitPositions.get("SIGNAL").getStartPos(), bitPositions.get("SIGNAL").getEndPos());

        System.out.printf("Best solution (fast, slow, signal): %d, %d, %d%n", fastPeriod, slowPeriod, signalPeriod);
        System.out.printf("GA execution time: %d milliseconds%n", (endTime - startTime));

        double profit = executeStandardTradeRules("GA");
        int fromPos = Math.max(Math.max(fastPeriod, slowPeriod), signalPeriod) + 2;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos, toPos);
        System.out.println("xOverHodl: " + (profit / hodlProfit) + " - profit: " + profit + " - hodlProfit: " + hodlProfit);
    }
    // End Agentic Workflow Section - 3. genetic algorithm search

    // Begin Agentic Workflow Section - 4. single candidate evaluation
    private static void singleCandidateEvaluation() throws IOException {
        TOKEN = "BTC";
        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\binance";
        DATA_RESOLUTION = 15;
        FEE_RATE = 0.001;
        FROM_DATE = "2020-07-24";
        TO_DATE = "2025-07-30";

        fastPeriod = 12;
        slowPeriod = 26;
        signalPeriod = 9;

        System.out.println("Begin Data Fetch: " + FROM_DATE);
        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        System.out.println("End Data Fetch: " + TO_DATE);

        double profit = executeStandardTradeRules("SCE");
        int fromPos = Math.max(Math.max(fastPeriod, slowPeriod), signalPeriod) + 2;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos, toPos);
        System.out.println("xOverHodl: " + (profit / hodlProfit) + " - profit: " + profit + " - hodlProfit: " + hodlProfit);
    }
    // End Agentic Workflow Section - 4. single candidate evaluation

    private static @NotNull String choicePrompt(String[] args) {
        if (args.length == 0) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Choose mode [1 = SCE (Single Candidate Evaluation), 2 = GA (Genetic Algorithm Search)]: ");
            return scanner.nextLine().trim();
        }
        return "";
    }

    // Begin Agentic Workflow Section - 5. execute standard trading rules
    public static double executeStandardTradeRules(String type) {
        TokenData tokenData = generateCustomSignals(fastPeriod, slowPeriod, signalPeriod);
        int fromPos = Math.max(Math.max(fastPeriod, slowPeriod), signalPeriod) + 2;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double profit = calculateProfit(fromPos, toPos, tokenData);
        if (type.equals("SCE")) TOKEN_DATA = tokenData;
        return profit;
    }
    // End Agentic Workflow Section - 5. execute standard trading rules

    // Begin Agentic Workflow Section - 6. generate custom signals
    public static TokenData generateCustomSignals(int fastPeriod, int slowPeriod, int signalPeriod) {
        TokenData tokenData = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        double[] close = ORIGINAL_DATA.getClose();
        int n = close.length;

        double[] emaFast = new double[n];
        double[] emaSlow = new double[n];
        double[] macd = new double[n];
        double[] signal = new double[n];
        double[] orderSignal = new double[n];

        Arrays.fill(emaFast, 0.0);
        Arrays.fill(emaSlow, 0.0);
        Arrays.fill(macd, 0.0);
        Arrays.fill(signal, 0.0);
        Arrays.fill(orderSignal, 0.0);

        // EMA init
        emaFast[0] = close[0];
        emaSlow[0] = close[0];

        double aFast = 2.0 / (fastPeriod + 1.0);
        double aSlow = 2.0 / (slowPeriod + 1.0);
        double aSig = 2.0 / (signalPeriod + 1.0);

        for (int i = 1; i < n; i++) {
            emaFast[i] = aFast * close[i] + (1.0 - aFast) * emaFast[i - 1];
            emaSlow[i] = aSlow * close[i] + (1.0 - aSlow) * emaSlow[i - 1];
            macd[i] = emaFast[i] - emaSlow[i];
            signal[i] = (i == 1) ? macd[i] : (aSig * macd[i] + (1.0 - aSig) * signal[i - 1]);
        }

        int start = Math.max(Math.max(fastPeriod, slowPeriod), signalPeriod) + 2;
        for (int i = start; i < n; i++) {
            boolean crossUp = macd[i - 1] <= signal[i - 1] && macd[i] > signal[i];
            boolean crossDown = macd[i - 1] >= signal[i - 1] && macd[i] < signal[i];

            if (crossUp) orderSignal[i] = BUY;
            else if (crossDown) orderSignal[i] = SELL;
        }

        tokenData.getData().put("EMAFast", emaFast);
        tokenData.getData().put("EMASlow", emaSlow);
        tokenData.getData().put("MACD", macd);
        tokenData.getData().put("Signal", signal);
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
            if (orderSignal[i] == BUY && !inPosition) {
                currentHoldings -= FEE_RATE * currentHoldings;
                tokenAmount = currentHoldings / closePrice[i];
                inPosition = true;
            } else if (orderSignal[i] == SELL && inPosition) {
                double sellValue = tokenAmount * closePrice[i];
                sellValue -= FEE_RATE * sellValue;
                currentHoldings = sellValue;
                tokenAmount = 0.0;
                inPosition = false;
            }
            holdingsVector[i] = inPosition ? tokenAmount * closePrice[i] : currentHoldings;
        }

        tokenData.getData().put("Holdings", holdingsVector);
        double finalHoldings = inPosition ? tokenAmount * closePrice[toPos] : currentHoldings;
        return finalHoldings - INITIAL_INVESTMENT;
    }
    // End Agentic Workflow Section - 7. calculate profit

    public static double calculateCompoundedReturnRate(int fromPos, int toPos, TokenData tokenData) {
        return 0.0;
    }

    public static double executeStandardModel(int fromPos, int toPos) {
        double begin = ORIGINAL_DATA.getClose()[fromPos];
        double end = ORIGINAL_DATA.getClose()[toPos];
        double profit = INITIAL_INVESTMENT * ((end - begin) / begin);
        double holdings = profit + INITIAL_INVESTMENT;
        holdings -= FEE_RATE * holdings;
        return profit;
    }

    private static int bitsToInt(BitString bitString, int start, int end) {
        int value = 0;
        for (int i = start; i < end; i++) {
            if (bitString.getBit(i)) value |= 1 << (i - start);
        }
        return value;
    }

    // Begin Agentic Workflow Section - 8. generate seed candidates
    private static List<BitString> generateSeedCandidates() {
        BitString seed = new BitString(bitPositions.getBitWidth());
        intToBits(seed, bitPositions.get("FAST").getStartPos(), bitPositions.get("FAST").getEndPos(), fastPeriod);
        intToBits(seed, bitPositions.get("SLOW").getStartPos(), bitPositions.get("SLOW").getEndPos(), slowPeriod);
        intToBits(seed, bitPositions.get("SIGNAL").getStartPos(), bitPositions.get("SIGNAL").getEndPos(), signalPeriod);
        return Collections.singletonList(seed);
    }
    // End Agentic Workflow Section - 8. generate seed candidates

    private static void intToBits(BitString bitString, int start, int end, int value) {
        for (int i = start; i < end; i++) {
            int bitIndex = i - start;
            bitString.setBit(i, ((value >> bitIndex) & 1) == 1);
        }
    }

    // Begin Agentic Workflow Section - 9. fitness function
    private static class FitnessFunction implements FitnessEvaluator<BitString> {
        @Override
        public double getFitness(BitString candidate, List<? extends BitString> population) {
            int fast = bitsToInt(candidate, bitPositions.get("FAST").getStartPos(), bitPositions.get("FAST").getEndPos());
            int slow = bitsToInt(candidate, bitPositions.get("SLOW").getStartPos(), bitPositions.get("SLOW").getEndPos());
            int sig = bitsToInt(candidate, bitPositions.get("SIGNAL").getStartPos(), bitPositions.get("SIGNAL").getEndPos());

            int fromPos = Math.max(Math.max(fast, slow), sig) + 2;

            // Guards
            if (fast < 2 || slow < 3 || sig < 2) return 0;
            if (fast >= slow) return 0;
            if (fromPos >= ORIGINAL_DATA.getClose().length) return 0;

            TokenData td = generateCustomSignals(fast, slow, sig);
            int toPos = ORIGINAL_DATA.getClose().length - 1;

            double profit = calculateProfit(fromPos, toPos, td);
            double hodlProfit = executeStandardModel(fromPos, toPos);

            if (FITNESS_METHOD == FitnessMethod.PROFIT) return Math.max(profit, 0);
            return Math.max(profit / hodlProfit, 0);
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
            if (rng.nextDouble() < probability.doubleValue()) return crossoverOperator.apply(population, rng);
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

            int fast = bitsToInt(bestCandidate, bitPositions.get("FAST").getStartPos(), bitPositions.get("FAST").getEndPos());
            int slow = bitsToInt(bestCandidate, bitPositions.get("SLOW").getStartPos(), bitPositions.get("SLOW").getEndPos());
            int sig = bitsToInt(bestCandidate, bitPositions.get("SIGNAL").getStartPos(), bitPositions.get("SIGNAL").getEndPos());

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            String formattedDate = now.format(formatter);

            System.out.printf(
                    "Generation %d: Best fitness: %.3f, Solution (fast, slow, sig): %d, %d, %d - %s%n",
                    data.getGenerationNumber(),
                    data.getBestCandidateFitness(),
                    fast, slow, sig, formattedDate
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
            for (int i = 0; i < populationSize; i++) population.add(generateRandomCandidate(rng));
            return population;
        }

        @Override
        public List<BitString> generateInitialPopulation(int populationSize, Collection<BitString> seedCandidates, Random rng) {
            List<BitString> population = new ArrayList<>(populationSize);
            population.addAll(seedCandidates);
            for (int i = seedCandidates.size(); i < populationSize; i++) population.add(generateRandomCandidate(rng));
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
            if (kv.length % 2 != 0) throw new IllegalArgumentException("Must pass even number of arguments");
            LinkedHashMap<String, BitField> m = new LinkedHashMap<>();
            int cursor = 0;
            for (int i = 0; i < kv.length; i += 2) {
                String key = (String) kv[i];
                int len = (Integer) kv[i + 1];
                BitField bf = new BitField(len, cursor);
                m.put(key, bf);
                cursor = bf.getEndPos();
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
