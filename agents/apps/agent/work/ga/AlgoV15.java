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
 * AlgoV15 (x4 params): Breakout with partial exit + trailing stop.
 *
 * Strategy:
 *  - rollingHigh(lookback)
 *  - BUY when close crosses above rollingHigh*(1+entryThresholdPct)
 *  - Once in position:
 *      - if close >= entryPrice*(1+partialProfitPct): sell PARTIAL_EXIT_FRACTION of tokens (one-time)
 *      - always maintain trailing stop: sell remaining if close < peak*(1-trailingStopPct)
 *
 * Searchable params:
 *  - lookback (int)
 *  - entryThresholdPct (double %)
 *  - partialProfitPct (double %)
 *  - trailingStopPct (double %)
 */
public class AlgoV15 {
    static final double INITIAL_INVESTMENT = 100;
    static final int BUY = 1;
    static final int SELL = -1;

    static final double PARTIAL_EXIT_FRACTION = 0.5; // fixed simplicity

    static String TOKEN;
    static String WORKING_DIRECTORY;
    static int DATA_RESOLUTION;
    static double FEE_RATE;
    static String FROM_DATE;
    static String TO_DATE;
    static TokenData ORIGINAL_DATA;
    static TokenData TOKEN_DATA;

    static public enum FitnessMethod {X_OVER_HODL, PROFIT}

    static FitnessMethod FITNESS_METHOD;
    static boolean GA_SEED;

    // Begin Agentic Workflow Section - 0. fixed variables
    // Partial exit fraction fixed.
    // End Agentic Workflow Section - 0. fixed variables

    // Begin Agentic Workflow Section - 1. input parameters
    static int lookback;
    static double entryThresholdPct;
    static double partialProfitPct;
    static double trailingStopPct;
    // End Agentic Workflow Section - 1. input parameters

    static final int POPULATION_SIZE = 2000;
    static final int MAX_GENERATIONS = 800;
    static final int RUN_SIZE = 100;
    static final double CROSSOVER_PROBABILITY = 0.9;
    static final double MUTATION_PROBABILITY = 0.2;
    static final int ELITISM_COUNT = (int) Math.max(1, Math.round(POPULATION_SIZE * 0.02));

    static int INDICATOR_DIVIDER = 100;

    // Begin Agentic Workflow Section - 2. bit positions
    static BitPositions bitPositions = BitPositions.assemble(
            "LOOKBACK", 8,
            "ENTRY_TH", 10,
            "PARTIAL_PROFIT", 10,
            "TRAIL_STOP", 10
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

        lookback = 40;
        entryThresholdPct = 0.5;
        partialProfitPct = 2.0;
        trailingStopPct = 2.0;

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

        System.out.println("GA start...");
        if (GA_SEED)
            engine.evolve(POPULATION_SIZE, ELITISM_COUNT, seedCandidates, new GenerationCount(MAX_GENERATIONS), stagnationTermination);
        else
            engine.evolve(POPULATION_SIZE, ELITISM_COUNT, new GenerationCount(MAX_GENERATIONS), stagnationTermination);
        System.out.println("GA end...\n");

        BitString best = progressListener.getBestCandidate();
        lookback = bitsToInt(best, bitPositions.get("LOOKBACK").getStartPos(), bitPositions.get("LOOKBACK").getEndPos());
        entryThresholdPct = bitsToInt(best, bitPositions.get("ENTRY_TH").getStartPos(), bitPositions.get("ENTRY_TH").getEndPos()) / (double) INDICATOR_DIVIDER;
        partialProfitPct = bitsToInt(best, bitPositions.get("PARTIAL_PROFIT").getStartPos(), bitPositions.get("PARTIAL_PROFIT").getEndPos()) / (double) INDICATOR_DIVIDER;
        trailingStopPct = bitsToInt(best, bitPositions.get("TRAIL_STOP").getStartPos(), bitPositions.get("TRAIL_STOP").getEndPos()) / (double) INDICATOR_DIVIDER;

        System.out.printf("Best (lb, entry%%, partial%%, trail%%): %d, %.2f%%, %.2f%%, %.2f%%%n",
                lookback, entryThresholdPct, partialProfitPct, trailingStopPct);

        double profit = executeStandardTradeRules("GA");
        int fromPos = lookback;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        System.out.println("xOverHodl: " + (profit / executeStandardModel(fromPos, toPos)));
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

        lookback = 40;
        entryThresholdPct = 0.5;
        partialProfitPct = 2.0;
        trailingStopPct = 2.0;

        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        double profit = executeStandardTradeRules("SCE");
        int fromPos = lookback;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        System.out.println("xOverHodl: " + (profit / executeStandardModel(fromPos, toPos)));
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
        TokenData td = generateCustomSignals(lookback, entryThresholdPct, partialProfitPct, trailingStopPct);
        int fromPos = lookback;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double profit = calculateProfit(fromPos, toPos, td);
        if (type.equals("SCE")) TOKEN_DATA = td;
        return profit;
    }
    // End Agentic Workflow Section - 5. execute standard trading rules

    // Begin Agentic Workflow Section - 6. generate custom signals
    public static TokenData generateCustomSignals(int lookback, double entryThresholdPct, double partialProfitPct, double trailingStopPct) {
        TokenData td = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        double[] close = ORIGINAL_DATA.getClose();
        int n = close.length;

        double[] rollingHigh = new double[n];
        double[] orderSignal = new double[n];
        Arrays.fill(rollingHigh, 0.0);
        Arrays.fill(orderSignal, 0.0);

        int start = Math.max(lookback, 2);
        for (int i = start; i < n; i++) {
            double high = Double.NEGATIVE_INFINITY;
            for (int j = i - lookback; j < i; j++) {
                if (close[j] > high) high = close[j];
            }
            rollingHigh[i] = high;
        }

        boolean inPosition = false;
        double entry = 0.0;
        double peak = 0.0;
        boolean partialExited = false;

        double entryTh = entryThresholdPct / 100.0;
        double partialTh = partialProfitPct / 100.0;
        double trail = trailingStopPct / 100.0;

        for (int i = start; i < n; i++) {
            if (!inPosition) {
                double level = rollingHigh[i] * (1.0 + entryTh);
                boolean crossUp = close[i - 1] <= level && close[i] > level;
                if (crossUp) {
                    orderSignal[i] = BUY;
                    inPosition = true;
                    entry = close[i];
                    peak = close[i];
                    partialExited = false;
                }
            } else {
                if (close[i] > peak) peak = close[i];

                // encode partial exit as SELL signal too (profit calc will implement partial)
                boolean partialHit = !partialExited && close[i] >= entry * (1.0 + partialTh);
                boolean trailHit = close[i] < peak * (1.0 - trail);

                if (partialHit) {
                    // Use SELL as a marker for partial. We store a separate series to disambiguate.
                    // (keeps backtest simple and still 1 signal channel)
                    td.getData().computeIfAbsent("Partial Exit", k -> new double[n]);
                    td.getData().get("Partial Exit")[i] = 1.0;
                    partialExited = true;
                }

                if (trailHit) {
                    orderSignal[i] = SELL;
                    inPosition = false;
                    entry = 0.0;
                    peak = 0.0;
                    partialExited = false;
                }
            }
        }

        td.getData().put("RollingHigh", rollingHigh);
        td.getData().put("Order Signal", orderSignal);
        if (!td.getData().containsKey("Partial Exit")) {
            td.getData().put("Partial Exit", new double[n]);
        }
        return td;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    // Begin Agentic Workflow Section - 7. calculate profit
    public static double calculateProfit(int fromPos, int toPos, TokenData td) {
        double currentCash = INITIAL_INVESTMENT;
        boolean inPosition = false;
        double tokenAmount = 0.0;

        double[] orderSignal = td.getData().get("Order Signal");
        double[] partial = td.getData().get("Partial Exit");
        double[] close = ORIGINAL_DATA.getClose();

        for (int i = fromPos; i <= toPos; i++) {
            if (orderSignal[i] == BUY && !inPosition) {
                currentCash -= FEE_RATE * currentCash;
                tokenAmount = currentCash / close[i];
                currentCash = 0.0;
                inPosition = true;

            } else {
                if (inPosition && partial[i] == 1.0) {
                    // sell fraction
                    double sellTokens = tokenAmount * PARTIAL_EXIT_FRACTION;
                    double sellValue = sellTokens * close[i];
                    sellValue -= FEE_RATE * sellValue;
                    currentCash += sellValue;
                    tokenAmount -= sellTokens;
                }

                if (orderSignal[i] == SELL && inPosition) {
                    double sellValue = tokenAmount * close[i];
                    sellValue -= FEE_RATE * sellValue;
                    currentCash += sellValue;
                    tokenAmount = 0.0;
                    inPosition = false;
                }
            }
        }

        double finalHoldings = currentCash + (inPosition ? tokenAmount * close[toPos] : 0.0);
        return finalHoldings - INITIAL_INVESTMENT;
    }
    // End Agentic Workflow Section - 7. calculate profit

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
        for (int i = start; i < end; i++) if (bitString.getBit(i)) value |= 1 << (i - start);
        return value;
    }

    // Begin Agentic Workflow Section - 8. generate seed candidates
    private static List<BitString> generateSeedCandidates() {
        BitString seed = new BitString(bitPositions.getBitWidth());
        intToBits(seed, bitPositions.get("LOOKBACK").getStartPos(), bitPositions.get("LOOKBACK").getEndPos(), lookback);
        intToBits(seed, bitPositions.get("ENTRY_TH").getStartPos(), bitPositions.get("ENTRY_TH").getEndPos(), (int) Math.round(entryThresholdPct * INDICATOR_DIVIDER));
        intToBits(seed, bitPositions.get("PARTIAL_PROFIT").getStartPos(), bitPositions.get("PARTIAL_PROFIT").getEndPos(), (int) Math.round(partialProfitPct * INDICATOR_DIVIDER));
        intToBits(seed, bitPositions.get("TRAIL_STOP").getStartPos(), bitPositions.get("TRAIL_STOP").getEndPos(), (int) Math.round(trailingStopPct * INDICATOR_DIVIDER));
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
            int lb = bitsToInt(candidate, bitPositions.get("LOOKBACK").getStartPos(), bitPositions.get("LOOKBACK").getEndPos());
            double entry = bitsToInt(candidate, bitPositions.get("ENTRY_TH").getStartPos(), bitPositions.get("ENTRY_TH").getEndPos()) / (double) INDICATOR_DIVIDER;
            double partial = bitsToInt(candidate, bitPositions.get("PARTIAL_PROFIT").getStartPos(), bitPositions.get("PARTIAL_PROFIT").getEndPos()) / (double) INDICATOR_DIVIDER;
            double trail = bitsToInt(candidate, bitPositions.get("TRAIL_STOP").getStartPos(), bitPositions.get("TRAIL_STOP").getEndPos()) / (double) INDICATOR_DIVIDER;

            if (lb < 5 || lb >= ORIGINAL_DATA.getClose().length) return 0;
            if (trail <= 0.0 || partial <= 0.0) return 0;

            TokenData td = generateCustomSignals(lb, entry, partial, trail);
            int fromPos = lb;
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

            int lb = bitsToInt(bestCandidate, bitPositions.get("LOOKBACK").getStartPos(), bitPositions.get("LOOKBACK").getEndPos());
            double entry = bitsToInt(bestCandidate, bitPositions.get("ENTRY_TH").getStartPos(), bitPositions.get("ENTRY_TH").getEndPos()) / (double) INDICATOR_DIVIDER;
            double partial = bitsToInt(bestCandidate, bitPositions.get("PARTIAL_PROFIT").getStartPos(), bitPositions.get("PARTIAL_PROFIT").getEndPos()) / (double) INDICATOR_DIVIDER;
            double trail = bitsToInt(bestCandidate, bitPositions.get("TRAIL_STOP").getStartPos(), bitPositions.get("TRAIL_STOP").getEndPos()) / (double) INDICATOR_DIVIDER;

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            String formatted = now.format(formatter);

            System.out.printf(
                    "Generation %d: Best fitness: %.3f, Solution (lb, entry%%, partial%%, trail%%): %d, %.2f%%, %.2f%%, %.2f%% - %s%n",
                    data.getGenerationNumber(),
                    data.getBestCandidateFitness(),
                    lb, entry, partial, trail,
                    formatted
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
            private final int startPos;
            private final int endPos;

            public BitField(int length, int startPos) {
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
