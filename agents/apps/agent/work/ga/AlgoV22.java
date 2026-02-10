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
 * AlgoV22: minimal variation from AlgoV16 with NO new parameters.
 * Small change: add a fixed hysteresis to SELL to reduce churn.
 * Specifically, sellCondition becomes: fast < avgGainMin * 1.05 AND slow > fast.
 * No new params: 1.05 is a fixed multiplier.
 */
public class AlgoV22 {
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
    static double STOP_LOSS;
    static double STOP_LOSS2;
    static double STOP_LOSS3;
    // End Agentic Workflow Section - 0. fixed variables

    // Begin Agentic Workflow Section - 1. input parameters
    static double avgLossMin;
    static double avgGainMin;
    static int period1;
    static int period2;
    static int period3;
    // End Agentic Workflow Section - 1. input parameters

    static boolean IS_AVG_GAIN_MIN_ONLY_POSITIVE = false;
    static final int POPULATION_SIZE = 2000;
    static final int MAX_GENERATIONS = 800;
    static final int RUN_SIZE = 100;
    static final double CROSSOVER_PROBABILITY = 0.9;
    static final double MUTATION_PROBABILITY = 0.2;
    static final int ELITISM_COUNT = (int) Math.max(1, Math.round(POPULATION_SIZE * 0.02));

    static int INDICATOR_DIVIDER = 1000;

    // Begin Agentic Workflow Section - 2. bit positions
    static BitPositions bitPositions = BitPositions.assemble(
            "AVG_LOSS_MIN", 6,
            "AVG_GAIN_MIN", 6,
            "PERIOD1", 10,
            "PERIOD2", 10,
            "PERIOD3", 10
    );
    // End Agentic Workflow Section - 2. bit positions

    public static void main(String[] args) throws IOException {
        String choice = choicePrompt(args);
        if ("1".equals(choice) || "SCE".equalsIgnoreCase(choice) || (args.length > 0 && "SCE".equalsIgnoreCase(args[0]))) {
            singleCandidateEvaluation();
        } else if ("2".equals(choice) || "GA".equalsIgnoreCase(choice) || (args.length > 0 && "GA".equalsIgnoreCase(args[0]))) {
            geneticAlgorithmSearch();
        } else {
            System.out.println("Invalid choice.");
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

        avgLossMin = 0.000;
        avgGainMin = -0.017;
        period1 = 944;
        period2 = 194;
        period3 = 516;

        STOP_LOSS = 0.0160;
        STOP_LOSS2 = 0.985;
        STOP_LOSS3 = 0.914;

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

        engine.evolve(POPULATION_SIZE, ELITISM_COUNT, new GenerationCount(MAX_GENERATIONS), stagnationTermination);

        BitString bestSolution = progressListener.getBestCandidate();
        decodeBest(bestSolution);

        double profit = executeStandardTradeRules("GA");
        int fromPos = (period1 + period3);
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        System.out.println("xOverHodl: " + (profit / executeStandardModel(fromPos - 1, toPos)));
    }
    // End Agentic Workflow Section - 3. genetic algorithm search

    private static void decodeBest(BitString bestSolution) {
        int rawValue = bitsToInt(bestSolution, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos());
        int bitLength = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos();
        double midpoint = Math.pow(2, bitLength) / 2.0;
        avgLossMin = (rawValue - midpoint) / INDICATOR_DIVIDER;

        int rawGainValue = bitsToInt(bestSolution, bitPositions.get("AVG_GAIN_MIN").getStartPos(), bitPositions.get("AVG_GAIN_MIN").getEndPos());
        int gainBitLength = bitPositions.get("AVG_GAIN_MIN").getEndPos() - bitPositions.get("AVG_GAIN_MIN").getStartPos();
        double gainMidpoint = Math.pow(2, gainBitLength) / 2.0;
        avgGainMin = IS_AVG_GAIN_MIN_ONLY_POSITIVE
                ? rawGainValue / INDICATOR_DIVIDER
                : (rawGainValue - gainMidpoint) / INDICATOR_DIVIDER;

        period1 = bitsToInt(bestSolution, bitPositions.get("PERIOD1").getStartPos(), bitPositions.get("PERIOD1").getEndPos());
        period2 = bitsToInt(bestSolution, bitPositions.get("PERIOD2").getStartPos(), bitPositions.get("PERIOD2").getEndPos());
        period3 = bitsToInt(bestSolution, bitPositions.get("PERIOD3").getStartPos(), bitPositions.get("PERIOD3").getEndPos());
    }

    // Begin Agentic Workflow Section - 4. single candidate evaluation
    private static void singleCandidateEvaluation() throws IOException {
        TOKEN = "BTC";
        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\binance";
        DATA_RESOLUTION = 15;
        FEE_RATE = 0.001;
        FROM_DATE = "2020-07-24";
        TO_DATE = "2025-07-30";

        avgLossMin = 0.000;
        avgGainMin = -0.029;
        period1 = 987;
        period2 = 162;
        period3 = 375;

        STOP_LOSS = 0.0160;
        STOP_LOSS2 = 0.985;
        STOP_LOSS3 = 0.914;

        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        double profit = executeStandardTradeRules("SCE");
        int fromPos = (period1 + period3);
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        System.out.println("xOverHodl: " + (profit / executeStandardModel(fromPos - 1, toPos)));
    }
    // End Agentic Workflow Section - 4. single candidate evaluation

    private static @NotNull String choicePrompt(String[] args) {
        if (args.length == 0) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Choose mode [1 = SCE, 2 = GA]: ");
            return scanner.nextLine().trim();
        }
        return "";
    }

    // Begin Agentic Workflow Section - 5. execute standard trading rules
    public static double executeStandardTradeRules(String type) {
        TokenData tokenData = generateCustomSignals(avgLossMin, avgGainMin, period1, period2, period3);
        int fromPos = (period1 + period3);
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double profit = calculateProfit(fromPos - 1, toPos, tokenData);
        if (type.equals("SCE")) TOKEN_DATA = tokenData;
        return profit;
    }
    // End Agentic Workflow Section - 5. execute standard trading rules

    // Begin Agentic Workflow Section - 6. generate custom signals
    public static TokenData generateCustomSignals(double avgLossMin, double avgGainMin, int period1, int period2, int period3) {
        TokenData tokenData = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);
        tokenData = updateSMA(period1, ORIGINAL_DATA.getClose(), tokenData);
        tokenData = updateFastAverageGainLoss(period1 + period2 - 1, period2, tokenData.getData().get("SMA"), tokenData);
        tokenData = updateSlowAverageGainLoss(period1 + period3 - 1, period3, tokenData.getData().get("SMA"), tokenData);
        tokenData = updateOrderSignal(avgLossMin, avgGainMin, period1 + period3 - 1, ORIGINAL_DATA.getClose(), tokenData, STOP_LOSS);
        return tokenData;
    }

    public static TokenData updateSMA(int period1, double[] closePrices, TokenData tokenData) {
        int n = closePrices.length;
        double[] sma = new double[n];
        double rollingSum = 0.0;
        for (int i = 0; i < n; i++) {
            if (i < period1 - 1) {
                sma[i] = 0.0;
                rollingSum += closePrices[i];
            } else {
                rollingSum += closePrices[i];
                sma[i] = rollingSum / period1;
                rollingSum -= closePrices[i - period1 + 1];
            }
        }
        tokenData.getData().put("SMA", sma);
        return tokenData;
    }

    public static TokenData updateFastAverageGainLoss(int startPos, int period, double[] smaValues, TokenData tokenData) {
        int n = smaValues.length;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            if (i < startPos || i - period < 0) v[i] = 0.0;
            else {
                double current = smaValues[i];
                double previous = smaValues[i - period];
                v[i] = (current != 0.0) ? (((current - previous) / period) / current * 100.0) : 0.0;
            }
        }
        tokenData.getData().put("Fast Avg Gain/Loss", v);
        return tokenData;
    }

    public static TokenData updateSlowAverageGainLoss(int startPos, int period, double[] smaValues, TokenData tokenData) {
        int n = smaValues.length;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            if (i < startPos || i - period < 0) v[i] = 0.0;
            else {
                double current = smaValues[i];
                double previous = smaValues[i - period];
                v[i] = (current != 0.0) ? (((current - previous) / period) / current * 100.0) : 0.0;
            }
        }
        tokenData.getData().put("Slow Avg Gain/Loss", v);
        return tokenData;
    }

    public static TokenData updateOrderSignal(double avgLossMin, double avgGainMin, int period, double[] closePrices, TokenData tokenData, double stopLoss) {
        int n = closePrices.length;
        double[] order = new double[n];
        final int HOLD = 0;

        double buySignalLastPrice = 0.0;
        double purchasePrice = 0.0;

        double[] fast = tokenData.getData().get("Fast Avg Gain/Loss");
        double[] slow = tokenData.getData().get("Slow Avg Gain/Loss");
        double[] sma = tokenData.getData().get("SMA");

        // Fixed hysteresis multiplier.
        final double SELL_HYST = 1.05;

        for (int i = period; i < n; i++) {
            double f = fast[i];
            double s = slow[i];
            double close = closePrices[i];
            double base = sma[i];

            boolean buyCondition = (f < avgLossMin && s < f) || (base != 0.0 && close / base < 0.69);
            boolean sellCondition = (f < (avgGainMin * SELL_HYST)) && (s > f); // <-- small change

            if (buyCondition) {
                order[i] = BUY;
                buySignalLastPrice = close;
                if (purchasePrice == 0.0) purchasePrice = close;
            } else if (sellCondition) {
                order[i] = SELL;
                purchasePrice = 0.0;
            } else {
                order[i] = HOLD;
                if (purchasePrice != 0.0 && close < buySignalLastPrice * (1.0 - stopLoss)) {
                    order[i] = SELL;
                    purchasePrice = 0.0;
                }
            }
        }

        tokenData.getData().put("Order Signal", order);
        return tokenData;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    // Begin Agentic Workflow Section - 7. calculate profit
    public static double calculateProfit(int fromPos, int toPos, TokenData tokenData) {
        double currentHoldings = INITIAL_INVESTMENT;
        boolean inPos = false;
        double tokenAmt = 0.0;

        double[] order = tokenData.getData().get("Order Signal");
        double[] close = ORIGINAL_DATA.getClose();

        for (int i = fromPos + 1; i <= toPos; i++) {
            if (Double.isNaN(order[i])) continue;
            if (order[i] == BUY && !inPos) {
                currentHoldings -= (FEE_RATE * currentHoldings);
                tokenAmt = currentHoldings / close[i];
                inPos = true;
            } else if (order[i] == SELL && inPos) {
                currentHoldings = tokenAmt * close[i];
                currentHoldings -= (FEE_RATE * currentHoldings);
                tokenAmt = 0.0;
                inPos = false;
            }
        }

        double finalHoldings = inPos ? tokenAmt * close[toPos] : currentHoldings;
        return finalHoldings - INITIAL_INVESTMENT;
    }
    // End Agentic Workflow Section - 7. calculate profit

    public static double executeStandardModel(int fromPos, int toPos) {
        double beginPrice = ORIGINAL_DATA.getClose()[fromPos];
        double endPrice = ORIGINAL_DATA.getClose()[toPos];
        double profit = INITIAL_INVESTMENT * ((endPrice - beginPrice) / beginPrice);
        double holdings = profit + INITIAL_INVESTMENT;
        holdings -= (FEE_RATE * holdings);
        return profit;
    }

    private static int bitsToInt(BitString bitString, int start, int end) {
        int value = 0;
        for (int i = start; i < end; i++) if (bitString.getBit(i)) value |= 1 << (i - start);
        return value;
    }

    private static void intToBits(BitString bitString, int start, int end, int value) {
        for (int i = start; i < end; i++) {
            int bitIndex = i - start;
            bitString.setBit(i, ((value >> bitIndex) & 1) == 1);
        }
    }

    // Begin Agentic Workflow Section - 8. generate seed candidates
    private static List<BitString> generateSeedCandidates() {
        BitString seed = new BitString(bitPositions.getBitWidth());

        int lossBits = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos();
        double lossMid = Math.pow(2, lossBits) / 2.0;
        intToBits(seed, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos(),
                (int) Math.round((avgLossMin * INDICATOR_DIVIDER) + lossMid));

        int gainBits = bitPositions.get("AVG_GAIN_MIN").getEndPos() - bitPositions.get("AVG_GAIN_MIN").getStartPos();
        double gainMid = Math.pow(2, gainBits) / 2.0;
        intToBits(seed, bitPositions.get("AVG_GAIN_MIN").getStartPos(), bitPositions.get("AVG_GAIN_MIN").getEndPos(),
                (int) Math.round((avgGainMin * INDICATOR_DIVIDER) + gainMid));

        intToBits(seed, bitPositions.get("PERIOD1").getStartPos(), bitPositions.get("PERIOD1").getEndPos(), period1);
        intToBits(seed, bitPositions.get("PERIOD2").getStartPos(), bitPositions.get("PERIOD2").getEndPos(), period2);
        intToBits(seed, bitPositions.get("PERIOD3").getStartPos(), bitPositions.get("PERIOD3").getEndPos(), period3);

        return Collections.singletonList(seed);
    }
    // End Agentic Workflow Section - 8. generate seed candidates

    // Begin Agentic Workflow Section - 9. fitness function
    private static class FitnessFunction implements FitnessEvaluator<BitString> {
        @Override
        public double getFitness(BitString candidate, List<? extends BitString> population) {
            int rawLoss = bitsToInt(candidate, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos());
            int lossBits = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos();
            double lossMid = Math.pow(2, lossBits) / 2.0;
            double avgLossMin = (rawLoss - lossMid) / INDICATOR_DIVIDER;

            int rawGain = bitsToInt(candidate, bitPositions.get("AVG_GAIN_MIN").getStartPos(), bitPositions.get("AVG_GAIN_MIN").getEndPos());
            int gainBits = bitPositions.get("AVG_GAIN_MIN").getEndPos() - bitPositions.get("AVG_GAIN_MIN").getStartPos();
            double gainMid = Math.pow(2, gainBits) / 2.0;
            double avgGainMin = IS_AVG_GAIN_MIN_ONLY_POSITIVE
                    ? rawGain / INDICATOR_DIVIDER
                    : (rawGain - gainMid) / INDICATOR_DIVIDER;

            int period1 = bitsToInt(candidate, bitPositions.get("PERIOD1").getStartPos(), bitPositions.get("PERIOD1").getEndPos());
            int period2 = bitsToInt(candidate, bitPositions.get("PERIOD2").getStartPos(), bitPositions.get("PERIOD2").getEndPos());
            int period3 = bitsToInt(candidate, bitPositions.get("PERIOD3").getStartPos(), bitPositions.get("PERIOD3").getEndPos());

            if (period1 < 2 || period2 < 2 || period3 < 2 || period2 > period3 || (period1 + period3) > ORIGINAL_DATA.getClose().length) return 0;

            TokenData tokenData = generateCustomSignals(avgLossMin, avgGainMin, period1, period2, period3);
            int fromPos = (period1 + period3);
            int toPos = ORIGINAL_DATA.getClose().length - 1;

            double profit = calculateProfit(fromPos - 1, toPos, tokenData);
            double hodlProfit = executeStandardModel(fromPos - 1, toPos);

            double score = (FITNESS_METHOD == FitnessMethod.PROFIT) ? profit : profit / hodlProfit;
            return Math.max(score, 0);
        }

        @Override
        public boolean isNatural() {
            return true;
        }
    }
    // End Agentic Workflow Section - 9. fitness function

    // Begin Agentic Workflow Section - 10. progress listener
    private static class ProgressListener implements EvolutionObserver<BitString> {
        private BitString bestCandidate;

        @Override
        public void populationUpdate(PopulationData<? extends BitString> data) {
            bestCandidate = data.getBestCandidate();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            System.out.printf("Gen %d best=%.3f %s%n", data.getGenerationNumber(), data.getBestCandidateFitness(), now.format(formatter));
        }

        public BitString getBestCandidate() {
            return bestCandidate;
        }
    }
    // End Agentic Workflow Section - 10. progress listener

    private static class ProbabilisticCrossover implements EvolutionaryOperator<BitString> {
        private final BitStringCrossover xo;
        private final Probability p;

        public ProbabilisticCrossover(int crossoverPoints, Probability probability) {
            this.xo = new BitStringCrossover(crossoverPoints);
            this.p = probability;
        }

        @Override
        public List<BitString> apply(List<BitString> population, Random rng) {
            return (rng.nextDouble() < p.doubleValue()) ? xo.apply(population, rng) : population;
        }
    }

    private static class StagnationTermination implements TerminationCondition {
        private final int max;
        private double last = Double.NEGATIVE_INFINITY;
        private int stagnant = 0;

        public StagnationTermination(int maxGenerationsWithoutImprovement) {
            this.max = maxGenerationsWithoutImprovement;
        }

        @Override
        public boolean shouldTerminate(PopulationData<?> populationData) {
            double cur = populationData.getBestCandidateFitness();
            if (cur > last) {
                last = cur;
                stagnant = 0;
            } else stagnant++;
            return stagnant >= max;
        }
    }

    private static class BitStringFactory implements CandidateFactory<BitString> {
        private final int length;

        public BitStringFactory(int length) {
            this.length = length;
        }

        @Override
        public BitString generateRandomCandidate(Random rng) {
            return new BitString(length, rng);
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
            LinkedHashMap<String, BitField> m = new LinkedHashMap<>();
            int cursor = 0;
            for (int i = 0; i < kv.length; i += 2) {
                String key = (String) kv[i];
                int len = (Integer) kv[i + 1];
                BitField f = new BitField(len, cursor);
                m.put(key, f);
                cursor = f.getEndPos();
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
