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
 * AlgoV13 (x4 params): Keltner channel breakout (EMA + ATR-style range) with stop-loss.
 *
 * Strategy:
 *   - EMA(period)
 *   - RangePct = 100 * (high-low)/close (proxy ATR in %)
 *   - ATRpct = SMA(rangePct, atrLookback)
 *   - Upper = EMA * (1 + k * ATRpct/100)
 *   - BUY  when close crosses above Upper
 *   - SELL when close crosses below EMA OR close <= entryPrice*(1-stopLossPct)
 *
 * Searchable params:
 *   - emaPeriod (int)
 *   - atrLookback (int)
 *   - k (double)
 *   - stopLossPct (double %)
 */
public class AlgoV13 {
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

    static public enum FitnessMethod {X_OVER_HODL, PROFIT}

    static FitnessMethod FITNESS_METHOD;
    static boolean GA_SEED;

    // Begin Agentic Workflow Section - 0. fixed variables
    // No fixed strategy params.
    // End Agentic Workflow Section - 0. fixed variables

    // Begin Agentic Workflow Section - 1. input parameters
    static int emaPeriod;
    static int atrLookback;
    static double k;
    static double stopLossPct;
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
            "EMA_PERIOD", 8,
            "ATR_LOOKBACK", 8,
            "K", 10,          // 0..10.23
            "STOP_LOSS", 10   // 0..10.23%
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

        emaPeriod = 40;
        atrLookback = 20;
        k = 2.0;
        stopLossPct = 2.0;

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
        emaPeriod = bitsToInt(best, bitPositions.get("EMA_PERIOD").getStartPos(), bitPositions.get("EMA_PERIOD").getEndPos());
        atrLookback = bitsToInt(best, bitPositions.get("ATR_LOOKBACK").getStartPos(), bitPositions.get("ATR_LOOKBACK").getEndPos());
        k = bitsToInt(best, bitPositions.get("K").getStartPos(), bitPositions.get("K").getEndPos()) / (double) INDICATOR_DIVIDER;
        stopLossPct = bitsToInt(best, bitPositions.get("STOP_LOSS").getStartPos(), bitPositions.get("STOP_LOSS").getEndPos()) / (double) INDICATOR_DIVIDER;

        System.out.printf("Best (emaP, atrLb, k, sl%%): %d, %d, %.2f, %.2f%%%n", emaPeriod, atrLookback, k, stopLossPct);

        double profit = executeStandardTradeRules("GA");
        int fromPos = Math.max(emaPeriod, atrLookback) + 2;
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

        emaPeriod = 40;
        atrLookback = 20;
        k = 2.0;
        stopLossPct = 2.0;

        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        double profit = executeStandardTradeRules("SCE");
        int fromPos = Math.max(emaPeriod, atrLookback) + 2;
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
        TokenData td = generateCustomSignals(emaPeriod, atrLookback, k, stopLossPct);
        int fromPos = Math.max(emaPeriod, atrLookback) + 2;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double profit = calculateProfit(fromPos, toPos, td);
        if (type.equals("SCE")) TOKEN_DATA = td;
        return profit;
    }
    // End Agentic Workflow Section - 5. execute standard trading rules

    // Begin Agentic Workflow Section - 6. generate custom signals
    public static TokenData generateCustomSignals(int emaPeriod, int atrLookback, double k, double stopLossPct) {
        TokenData td = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        double[] close = ORIGINAL_DATA.getClose();
        double[] high = ORIGINAL_DATA.getHigh();
        double[] low = ORIGINAL_DATA.getLow();
        int n = close.length;

        double[] ema = new double[n];
        double[] rangePct = new double[n];
        double[] atrPct = new double[n];
        double[] upper = new double[n];
        double[] orderSignal = new double[n];

        Arrays.fill(ema, 0.0);
        Arrays.fill(rangePct, 0.0);
        Arrays.fill(atrPct, 0.0);
        Arrays.fill(upper, 0.0);
        Arrays.fill(orderSignal, 0.0);

        // EMA
        ema[0] = close[0];
        double a = 2.0 / (emaPeriod + 1.0);
        for (int i = 1; i < n; i++) {
            ema[i] = a * close[i] + (1.0 - a) * ema[i - 1];
        }

        for (int i = 0; i < n; i++) {
            if (close[i] != 0.0) {
                rangePct[i] = 100.0 * (high[i] - low[i]) / close[i];
            }
        }

        // ATR proxy (SMA of rangePct)
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            sum += rangePct[i];
            if (i >= atrLookback) sum -= rangePct[i - atrLookback];
            if (i >= atrLookback - 1) atrPct[i] = sum / atrLookback;
        }

        for (int i = 0; i < n; i++) {
            upper[i] = ema[i] * (1.0 + k * (atrPct[i] / 100.0));
        }

        boolean inPosition = false;
        double entry = 0.0;
        double sl = stopLossPct / 100.0;

        int start = Math.max(Math.max(emaPeriod, atrLookback), 2);
        for (int i = start; i < n; i++) {
            if (!inPosition) {
                boolean buyCross = close[i - 1] <= upper[i - 1] && close[i] > upper[i];
                if (buyCross) {
                    orderSignal[i] = BUY;
                    inPosition = true;
                    entry = close[i];
                }
            } else {
                boolean crossDown = close[i - 1] >= ema[i - 1] && close[i] < ema[i];
                boolean slHit = close[i] <= entry * (1.0 - sl);
                if (crossDown || slHit) {
                    orderSignal[i] = SELL;
                    inPosition = false;
                    entry = 0.0;
                }
            }
        }

        td.getData().put("EMA", ema);
        td.getData().put("ATRpct", atrPct);
        td.getData().put("Upper", upper);
        td.getData().put("Order Signal", orderSignal);
        return td;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    // Begin Agentic Workflow Section - 7. calculate profit
    public static double calculateProfit(int fromPos, int toPos, TokenData td) {
        double currentHoldings = INITIAL_INVESTMENT;
        boolean inPosition = false;
        double tokenAmount = 0.0;

        double[] orderSignal = td.getData().get("Order Signal");
        double[] close = ORIGINAL_DATA.getClose();

        for (int i = fromPos; i <= toPos; i++) {
            if (orderSignal[i] == BUY && !inPosition) {
                currentHoldings -= FEE_RATE * currentHoldings;
                tokenAmount = currentHoldings / close[i];
                inPosition = true;
            } else if (orderSignal[i] == SELL && inPosition) {
                double sellValue = tokenAmount * close[i];
                sellValue -= FEE_RATE * sellValue;
                currentHoldings = sellValue;
                tokenAmount = 0.0;
                inPosition = false;
            }
        }

        double finalHoldings = inPosition ? tokenAmount * close[toPos] : currentHoldings;
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
        intToBits(seed, bitPositions.get("EMA_PERIOD").getStartPos(), bitPositions.get("EMA_PERIOD").getEndPos(), emaPeriod);
        intToBits(seed, bitPositions.get("ATR_LOOKBACK").getStartPos(), bitPositions.get("ATR_LOOKBACK").getEndPos(), atrLookback);
        intToBits(seed, bitPositions.get("K").getStartPos(), bitPositions.get("K").getEndPos(), (int) Math.round(k * INDICATOR_DIVIDER));
        intToBits(seed, bitPositions.get("STOP_LOSS").getStartPos(), bitPositions.get("STOP_LOSS").getEndPos(), (int) Math.round(stopLossPct * INDICATOR_DIVIDER));
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
            int eP = bitsToInt(candidate, bitPositions.get("EMA_PERIOD").getStartPos(), bitPositions.get("EMA_PERIOD").getEndPos());
            int aLb = bitsToInt(candidate, bitPositions.get("ATR_LOOKBACK").getStartPos(), bitPositions.get("ATR_LOOKBACK").getEndPos());
            double kk = bitsToInt(candidate, bitPositions.get("K").getStartPos(), bitPositions.get("K").getEndPos()) / (double) INDICATOR_DIVIDER;
            double slPct = bitsToInt(candidate, bitPositions.get("STOP_LOSS").getStartPos(), bitPositions.get("STOP_LOSS").getEndPos()) / (double) INDICATOR_DIVIDER;

            int fromPos = Math.max(eP, aLb) + 2;
            if (eP < 2 || aLb < 2 || fromPos >= ORIGINAL_DATA.getClose().length) return 0;
            if (kk <= 0.0 || slPct <= 0.0) return 0;

            TokenData td = generateCustomSignals(eP, aLb, kk, slPct);
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

            int eP = bitsToInt(bestCandidate, bitPositions.get("EMA_PERIOD").getStartPos(), bitPositions.get("EMA_PERIOD").getEndPos());
            int aLb = bitsToInt(bestCandidate, bitPositions.get("ATR_LOOKBACK").getStartPos(), bitPositions.get("ATR_LOOKBACK").getEndPos());
            double kk = bitsToInt(bestCandidate, bitPositions.get("K").getStartPos(), bitPositions.get("K").getEndPos()) / (double) INDICATOR_DIVIDER;
            double slPct = bitsToInt(bestCandidate, bitPositions.get("STOP_LOSS").getStartPos(), bitPositions.get("STOP_LOSS").getEndPos()) / (double) INDICATOR_DIVIDER;

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            String formatted = now.format(formatter);

            System.out.printf(
                    "Generation %d: Best fitness: %.3f, Solution (emaP, atrLb, k, sl%%): %d, %d, %.2f, %.2f%% - %s%n",
                    data.getGenerationNumber(),
                    data.getBestCandidateFitness(),
                    eP, aLb, kk, slPct,
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
