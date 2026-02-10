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
 * AlgoV9: simple 3-parameter Donchian channel breakout with trailing stop.
 *
 * Strategy:
 *   - HighN = highest(close, channelPeriod)
 *   - LowN  = lowest(close, channelPeriod)
 *   - BUY  when close crosses above HighN
 *   - SELL when close drops from peak by trailingStopPct
 *
 * Searchable params:
 *   - channelPeriod (int)
 *   - trailingStopPct (double %)
 *   - reentryDelay (int bars) (cooldown after sell)
 */
public class AlgoV9 {
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
    static int channelPeriod;
    static double trailingStopPct;
    static int reentryDelay;
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
            // 8 bits -> 0..255 (guard >=5)
            "CHANNEL_PERIOD", 8,
            // 10 bits -> 0..1023 => 0.00%..10.23%
            "TRAIL_STOP_PCT", 10,
            // 6 bits -> 0..63
            "REENTRY_DELAY", 6
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

        channelPeriod = 40;
        trailingStopPct = 2.0;
        reentryDelay = 5;

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

        BitString best = progressListener.getBestCandidate();

        channelPeriod = bitsToInt(best, bitPositions.get("CHANNEL_PERIOD").getStartPos(), bitPositions.get("CHANNEL_PERIOD").getEndPos());
        trailingStopPct = bitsToInt(best, bitPositions.get("TRAIL_STOP_PCT").getStartPos(), bitPositions.get("TRAIL_STOP_PCT").getEndPos()) / (double) INDICATOR_DIVIDER;
        reentryDelay = bitsToInt(best, bitPositions.get("REENTRY_DELAY").getStartPos(), bitPositions.get("REENTRY_DELAY").getEndPos());

        System.out.printf("Best solution (channelPeriod, trailingStopPct%%, reentryDelay): %d, %.2f%%, %d%n",
                channelPeriod, trailingStopPct, reentryDelay);
        System.out.printf("GA execution time: %d milliseconds%n", (endTime - startTime));

        double profit = executeStandardTradeRules("GA");
        int fromPos = channelPeriod;
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

        channelPeriod = 40;
        trailingStopPct = 2.0;
        reentryDelay = 5;

        System.out.println("Begin Data Fetch: " + FROM_DATE);
        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        System.out.println("End Data Fetch: " + TO_DATE);

        double profit = executeStandardTradeRules("SCE");
        int fromPos = channelPeriod;
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
        TokenData tokenData = generateCustomSignals(channelPeriod, trailingStopPct, reentryDelay);
        int fromPos = channelPeriod;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double profit = calculateProfit(fromPos, toPos, tokenData);
        if (type.equals("SCE")) TOKEN_DATA = tokenData;
        return profit;
    }
    // End Agentic Workflow Section - 5. execute standard trading rules

    // Begin Agentic Workflow Section - 6. generate custom signals
    public static TokenData generateCustomSignals(int channelPeriod, double trailingStopPct, int reentryDelay) {
        TokenData tokenData = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        double[] close = ORIGINAL_DATA.getClose();
        int n = close.length;

        double[] highN = new double[n];
        double[] lowN = new double[n];
        double[] orderSignal = new double[n];
        Arrays.fill(highN, 0.0);
        Arrays.fill(lowN, 0.0);
        Arrays.fill(orderSignal, 0.0);

        boolean inPosition = false;
        double peak = 0.0;
        int cooldown = 0;
        double stopFrac = trailingStopPct / 100.0;

        int start = Math.max(channelPeriod, 2);
        for (int i = start; i < n; i++) {
            // update channels using previous channelPeriod closes
            double h = Double.NEGATIVE_INFINITY;
            double l = Double.POSITIVE_INFINITY;
            for (int j = i - channelPeriod; j < i; j++) {
                double c = close[j];
                if (c > h) h = c;
                if (c < l) l = c;
            }
            highN[i] = h;
            lowN[i] = l;

            if (cooldown > 0) cooldown--;

            if (!inPosition && cooldown == 0) {
                boolean buyCross = close[i - 1] <= highN[i - 1] && close[i] > highN[i];
                if (buyCross) {
                    orderSignal[i] = BUY;
                    inPosition = true;
                    peak = close[i];
                }
            } else if (inPosition) {
                if (close[i] > peak) peak = close[i];
                boolean stopHit = close[i] < peak * (1.0 - stopFrac);
                if (stopHit) {
                    orderSignal[i] = SELL;
                    inPosition = false;
                    peak = 0.0;
                    cooldown = reentryDelay;
                }
            }
        }

        tokenData.getData().put("HighN", highN);
        tokenData.getData().put("LowN", lowN);
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
        intToBits(seed, bitPositions.get("CHANNEL_PERIOD").getStartPos(), bitPositions.get("CHANNEL_PERIOD").getEndPos(), channelPeriod);
        intToBits(seed, bitPositions.get("TRAIL_STOP_PCT").getStartPos(), bitPositions.get("TRAIL_STOP_PCT").getEndPos(),
                (int) Math.round(trailingStopPct * INDICATOR_DIVIDER));
        intToBits(seed, bitPositions.get("REENTRY_DELAY").getStartPos(), bitPositions.get("REENTRY_DELAY").getEndPos(), reentryDelay);
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
            int p = bitsToInt(candidate, bitPositions.get("CHANNEL_PERIOD").getStartPos(), bitPositions.get("CHANNEL_PERIOD").getEndPos());
            double stopPct = bitsToInt(candidate, bitPositions.get("TRAIL_STOP_PCT").getStartPos(), bitPositions.get("TRAIL_STOP_PCT").getEndPos()) / (double) INDICATOR_DIVIDER;
            int delay = bitsToInt(candidate, bitPositions.get("REENTRY_DELAY").getStartPos(), bitPositions.get("REENTRY_DELAY").getEndPos());

            if (p < 5 || p >= ORIGINAL_DATA.getClose().length) return 0;
            if (stopPct <= 0.0 || stopPct > 15.0) return 0;

            TokenData td = generateCustomSignals(p, stopPct, delay);
            int fromPos = p;
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

            int p = bitsToInt(bestCandidate, bitPositions.get("CHANNEL_PERIOD").getStartPos(), bitPositions.get("CHANNEL_PERIOD").getEndPos());
            double stopPct = bitsToInt(bestCandidate, bitPositions.get("TRAIL_STOP_PCT").getStartPos(), bitPositions.get("TRAIL_STOP_PCT").getEndPos()) / (double) INDICATOR_DIVIDER;
            int delay = bitsToInt(bestCandidate, bitPositions.get("REENTRY_DELAY").getStartPos(), bitPositions.get("REENTRY_DELAY").getEndPos());

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            String formattedDate = now.format(formatter);

            System.out.printf(
                    "Generation %d: Best fitness: %.3f, Solution (period, stopPct%%, delay): %d, %.2f%%, %d - %s%n",
                    data.getGenerationNumber(),
                    data.getBestCandidateFitness(),
                    p, stopPct, delay, formattedDate
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
