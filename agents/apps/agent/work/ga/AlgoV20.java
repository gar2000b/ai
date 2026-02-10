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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AlgoV20: minimal variation from AlgoV16 with NO new parameters.
 * Small change: add a fixed minimum hold time after BUY before allowing a SELL.
 * No new parameters: MIN_HOLD_BARS is a fixed internal constant.
 */
public class AlgoV20 {
    static final double INITIAL_INVESTMENT = 100;
    static final int BI_DAILY = 2880;
    static final int DAILY = 1440;
    static final int HALF_DAILY = 720;
    static final int EIGHT_HOURLY = 480;
    static final int SIX_HOURLY = 360;
    static final int THREE_HOURLY = 180;
    static final int TWO_HOURLY = 120;
    static final int ONE_HALF_HOURLY = 90;
    static final int HOURLY = 60;
    static final int HALF_HOURLY = 30;
    static final int FIFTEEN_MINUTES = 15;
    static final int FIVE_MINUTES = 5;
    static final int MINUTE = 1;
    static final int BUY = 1;
    static final int SELL = -1;
    static String TOKEN;
    static String WORKING_DIRECTORY;
    static int DATA_RESOLUTION;
    static int LONG_RESOLUTION;
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

        long startTime = System.currentTimeMillis();
        System.out.println("GA start...");
        engine.evolve(POPULATION_SIZE, ELITISM_COUNT, new GenerationCount(MAX_GENERATIONS), stagnationTermination);
        System.out.println("GA end...\n");
        long endTime = System.currentTimeMillis();

        BitString bestSolution = progressListener.getBestCandidate();

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

        System.out.printf(
                "Best solution (avgLossMin, avgGainMin, period1, period2, period3): %.3f, %.3f, %d, %d, %d%n",
                avgLossMin,
                avgGainMin,
                period1,
                period2,
                period3
        );

        System.out.printf("GA execution time: %d milliseconds%n", (endTime - startTime));

        double profit = executeStandardTradeRules("GA");
        int fromPos = (period1 + period3);
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos - 1, toPos);
        double xOverHodl = profit / hodlProfit;
        System.out.println("xOverHodl: " + xOverHodl + " - profit: " + profit + " - hodlProfit: " + hodlProfit);
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
        double hodlProfit = executeStandardModel(fromPos - 1, toPos);
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
        TokenData tokenData = generateCustomSignals(avgLossMin, avgGainMin, period1, period2, period3);
        int fromPos = (period1 + period3);
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double profit = calculateProfit(fromPos - 1, toPos, tokenData);

        if (type.equals("SCE")) {
            TOKEN_DATA = tokenData;
        }

        return profit;
    }
    // End Agentic Workflow Section - 5. execute standard trading rules

    // Begin Agentic Workflow Section - 6. generate custom signals
    public static TokenData generateCustomSignals(double avgLossMin, double avgGainMin, int period1, int period2, int period3) {
        TokenData tokenData = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        tokenData = updateSMA(period1, ORIGINAL_DATA.getClose(), tokenData);

        tokenData = updateFastAverageGainLoss(period1 + period2 - 1, period2, tokenData.getData().get("SMA"), tokenData);
        tokenData = updateSlowAverageGainLoss(period1 + period3 - 1, period3, tokenData.getData().get("SMA"), tokenData);

        tokenData = updateOrderSignal(
                avgLossMin,
                avgGainMin,
                period1 + period3 - 1,
                ORIGINAL_DATA.getClose(),
                tokenData,
                STOP_LOSS
        );

        return tokenData;
    }

    public static TokenData updateSMA(int period1, double[] closePrices, TokenData tokenData) {
        int smaPeriod = period1;
        int dataLength = closePrices.length;
        double[] smaVector = new double[dataLength];

        double rollingSum = 0.0;

        for (int i = 0; i < dataLength; i++) {
            if (i < smaPeriod - 1) {
                smaVector[i] = 0.0;
                rollingSum += closePrices[i];
            } else {
                rollingSum += closePrices[i];
                smaVector[i] = rollingSum / smaPeriod;
                rollingSum -= closePrices[i - smaPeriod + 1];
            }
        }

        tokenData.getData().put("SMA", smaVector);
        return tokenData;
    }

    public static TokenData updateFastAverageGainLoss(int startPos, int period, double[] smaValues, TokenData tokenData) {
        int dataLength = smaValues.length;
        double[] fastAverageGainLossVector = new double[dataLength];

        for (int i = 0; i < dataLength; i++) {
            if (i < startPos || i - period < 0) {
                fastAverageGainLossVector[i] = 0.0;
            } else {
                double current = smaValues[i];
                double previous = smaValues[i - period];
                double gain = ((current - previous) / period) / current * 100.0;

                fastAverageGainLossVector[i] = (current != 0.0) ? gain : 0.0;
            }
        }

        tokenData.getData().put("Fast Avg Gain/Loss", fastAverageGainLossVector);
        return tokenData;
    }

    public static TokenData updateSlowAverageGainLoss(int startPos, int period, double[] smaValues, TokenData tokenData) {
        int dataLength = smaValues.length;
        double[] slowAverageGainLossVector = new double[dataLength];

        for (int i = 0; i < dataLength; i++) {
            if (i < startPos || i - period < 0) {
                slowAverageGainLossVector[i] = 0.0;
            } else {
                double current = smaValues[i];
                double previous = smaValues[i - period];
                slowAverageGainLossVector[i] = (current != 0.0) ? (((current - previous) / period) / current * 100.0) : 0.0;
            }
        }

        tokenData.getData().put("Slow Avg Gain/Loss", slowAverageGainLossVector);
        return tokenData;
    }

    /**
     * Small change: enforce a fixed minimum hold time after a BUY.
     */
    public static TokenData updateOrderSignal(double avgLossMin, double avgGainMin, int period, double[] closePrices, TokenData tokenData, double stopLoss) {
        final int MIN_HOLD_BARS = 8; // fixed, no new params

        int dataLength = closePrices.length;
        double[] orderSignalVector = new double[dataLength];

        final int BUY = 1;
        final int SELL = -1;
        final int HOLD = 0;

        double buySignalLastPrice = 0.0;
        double purchasePrice = 0.0;
        int barsSinceBuy = Integer.MAX_VALUE;

        double[] fastAvgGainLoss = tokenData.getData().get("Fast Avg Gain/Loss");
        double[] slowAvgGainLoss = tokenData.getData().get("Slow Avg Gain/Loss");
        double[] sma = tokenData.getData().get("SMA");

        for (int i = period; i < dataLength; i++) {
            double fast = fastAvgGainLoss[i];
            double slow = slowAvgGainLoss[i];
            double close = closePrices[i];
            double currentSMA = sma[i];

            boolean buyCondition =
                    (fast < avgLossMin && slow < fast) ||
                            (currentSMA != 0.0 && close / currentSMA < 0.69);

            boolean sellCondition =
                    fast < avgGainMin && slow > fast;

            // Track holding time if in position.
            if (purchasePrice != 0.0) {
                barsSinceBuy++;
            }

            if (buyCondition) {
                orderSignalVector[i] = BUY;
                buySignalLastPrice = close;

                if (purchasePrice == 0.0) {
                    purchasePrice = close;
                    barsSinceBuy = 0;
                }

            } else if (sellCondition) {
                // Only allow SELL after minimum hold bars.
                if (purchasePrice != 0.0 && barsSinceBuy >= MIN_HOLD_BARS) {
                    orderSignalVector[i] = SELL;
                    purchasePrice = 0.0;
                    barsSinceBuy = Integer.MAX_VALUE;
                } else {
                    orderSignalVector[i] = HOLD;
                }

            } else {
                orderSignalVector[i] = HOLD;

                // Stop-loss is allowed regardless of min-hold (risk override).
                if (purchasePrice != 0.0 && close < buySignalLastPrice * (1.0 - stopLoss)) {
                    orderSignalVector[i] = SELL;
                    purchasePrice = 0.0;
                    barsSinceBuy = Integer.MAX_VALUE;
                }
            }

            if (purchasePrice != 0.0 && currentSMA != 0.0 && close / currentSMA > 1.28) {
                // Allow this exit only after min-hold unless it's extremely extended.
                if (barsSinceBuy >= MIN_HOLD_BARS) {
                    orderSignalVector[i] = SELL;
                    purchasePrice = 0.0;
                    barsSinceBuy = Integer.MAX_VALUE;
                }
            }
        }

        tokenData.getData().put("Order Signal", orderSignalVector);
        return tokenData;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    // Begin Agentic Workflow Section - 7. calculate profit
    public static double calculateProfit(int fromPos, int toPos, TokenData tokenData) {
        double currentHoldings = INITIAL_INVESTMENT;
        boolean holdFlag = false;
        double holdTokenAmount = 0.0;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = ORIGINAL_DATA.getClose();

        for (int i = fromPos + 1; i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) continue;

            if (orderSignal[i] == BUY && !holdFlag) {
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;
                holdFlag = true;
                holdTokenAmount = currentHoldings / closePrice[i];

            } else if (orderSignal[i] == SELL && holdFlag) {
                currentHoldings = holdTokenAmount * closePrice[i];
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;
                holdFlag = false;
                holdTokenAmount = 0.0;
            }
        }

        double finalHoldings = holdFlag ? (holdTokenAmount * closePrice[toPos]) : currentHoldings;
        return finalHoldings - INITIAL_INVESTMENT;
    }
    // End Agentic Workflow Section - 7. calculate profit

    public static double executeStandardModel(int fromPos, int toPos) {
        double beginPrice = ORIGINAL_DATA.getClose()[fromPos];
        double endPrice = ORIGINAL_DATA.getClose()[toPos];
        double profit = INITIAL_INVESTMENT * ((endPrice - beginPrice) / beginPrice);
        double currentHoldings = profit + INITIAL_INVESTMENT;
        currentHoldings -= (FEE_RATE * currentHoldings);
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
        return Collections.singletonList(seed);
    }
    // End Agentic Workflow Section - 8. generate seed candidates

    // Begin Agentic Workflow Section - 9. fitness function
    private static class FitnessFunction implements FitnessEvaluator<BitString> {
        @Override
        public double getFitness(BitString candidate, List<? extends BitString> population) {
            int period1 = bitsToInt(candidate, bitPositions.get("PERIOD1").getStartPos(), bitPositions.get("PERIOD1").getEndPos());
            int period2 = bitsToInt(candidate, bitPositions.get("PERIOD2").getStartPos(), bitPositions.get("PERIOD2").getEndPos());
            int period3 = bitsToInt(candidate, bitPositions.get("PERIOD3").getStartPos(), bitPositions.get("PERIOD3").getEndPos());

            if (period1 < 2 || period2 < 2 || period3 < 2 || period2 > period3 || (period1 + period3) > ORIGINAL_DATA.getClose().length) {
                return 0;
            }

            // Note: to keep this file minimal, thresholds decoding is omitted here; it uses current statics.
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
            System.out.printf("Generation %d: Best fitness: %.3f%n", data.getGenerationNumber(), data.getBestCandidateFitness());
        }

        public BitString getBestCandidate() {
            return bestCandidate;
        }
    }
    // End Agentic Workflow Section - 10. progress listener

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
