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

public class AlgoV1 {
    /**
     * Global vars and constants.
     */
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
    static double[] linearScale;

    static public enum FitnessMethod {X_OVER_HODL, PROFIT, COMPOUND_RETURN_RATE}

    static FitnessMethod FITNESS_METHOD;
    static boolean GA_SEED;

    // Begin Agentic Workflow Section - 0. fixed variables
    // No hard fixed strategy params for this simplified concept.
    // Risk/fee controls are handled by the shared backtest (FEE_RATE) and standard buy/sell mechanics.
    // End Agentic Workflow Section - 0. fixed variables

    // Begin Agentic Workflow Section - 1. input parameters
    // Searchable params (simple breakout strategy)
    static int lookbackPeriod;          // window for rolling high/low
    static double breakoutThresholdPct; // breakout trigger as % (e.g. 0.5 == 0.5%)
    // End Agentic Workflow Section - 1. input parameters

    /**
     * GA params.
     */
    static boolean IS_AVG_GAIN_MIN_ONLY_POSITIVE = false;
    static final int POPULATION_SIZE = 2000;
    static final int MAX_GENERATIONS = 800;
    static final int RUN_SIZE = 100;
    static final double CROSSOVER_PROBABILITY = 0.9;
    static final double MUTATION_PROBABILITY = 0.2;
    static final int ELITISM_COUNT = (int) Math.max(1, Math.round(POPULATION_SIZE * 0.02));

    /**
     * Global constants for param "bit width" boundaries for GA.
     */
    static int INDICATOR_DIVIDER = 1000;

    /**
     * Defines the bit layout for the GA chromosome by listing each parameter name
     * followed by the number of bits it occupies. The positions are assigned
     * sequentially in the order given.
     *
     * Example:
     *   "AVG_LOSS_MIN", 6  -> uses bits [0..6)
     *   "AVG_GAIN_MIN", 6  -> uses bits [6..12)
     *   "PERIOD1",      10 -> uses bits [12..22)
     *   "PERIOD2",      10 -> uses bits [22..32)
     *   "PERIOD3",      10 -> uses bits [32..42)
     *
     * The total chromosome width is the sum of all lengths (42 bits in this case).
     */
    // Begin Agentic Workflow Section - 2. bit positions
    static BitPositions bitPositions = BitPositions.assemble(
            // 6 bits -> 0..63 (we will clamp to >=2 in fitness)
            "LOOKBACK_PERIOD", 6,
            // 10 bits -> 0..1023 mapped to 0.0%..10.23% (via INDICATOR_DIVIDER=100)
            "BREAKOUT_THRESHOLD", 10
    );
    // End Agentic Workflow Section - 2. bit positions

    /**
     * This is our main point of entry for executing either a single candidate evaluation (SCE) or a
     * Genetic Algorithm (GA) search.
     *
     * @param args
     */
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

        // Objective Function Search Space Params (seed values)
        lookbackPeriod = 40;
        breakoutThresholdPct = 0.7; // percent

        // GA_SEED switch (uses search space params plugged in above)
        GA_SEED = false;
        FITNESS_METHOD = FitnessMethod.X_OVER_HODL;

        // Other
        // Using 100 so our raw threshold maps to percent with 2dp-ish resolution.
        // raw(0..1023)/100 => 0.00%..10.23%
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

        lookbackPeriod = bitsToInt(bestSolution,
                bitPositions.get("LOOKBACK_PERIOD").getStartPos(),
                bitPositions.get("LOOKBACK_PERIOD").getEndPos());

        int rawThr = bitsToInt(bestSolution,
                bitPositions.get("BREAKOUT_THRESHOLD").getStartPos(),
                bitPositions.get("BREAKOUT_THRESHOLD").getEndPos());
        breakoutThresholdPct = rawThr / (double) INDICATOR_DIVIDER;

        System.out.printf(
                "Best solution (lookbackPeriod, breakoutThresholdPct%%): %d, %.2f%%%n",
                lookbackPeriod,
                breakoutThresholdPct
        );

        System.out.printf("GA execution time: %d milliseconds%n", (endTime - startTime));

        double profit = executeStandardTradeRules("GA");
        int fromPos = lookbackPeriod;
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

        // Params (single run)
        lookbackPeriod = 40;
        breakoutThresholdPct = 0.7; // percent

        System.out.println("Begin Data Fetch: " + FROM_DATE);
        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        System.out.println("End Data Fetch: " + TO_DATE);
        System.out.println(ORIGINAL_DATA.getTokenSymbol());

        double profit = executeStandardTradeRules("SCE");
        int fromPos = lookbackPeriod;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos, toPos);
        double xOverHodl = profit / hodlProfit;
        System.out.println("xOverHodl: " + xOverHodl + " - profit: " + profit + " - hodlProfit: " + hodlProfit);

//        writePdmaGaCsv(TOKEN, WORKING_DIRECTORY, ORIGINAL_DATA, TOKEN_DATA);
//        System.out.println("*** PdmaGaCsv Chart written to disk ***");
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

    public static void writePdmaGaCsv(String tokenSymbol,
                                      String workingDirectory,
                                      TokenData originalData,
                                      TokenData tokenData) throws IOException {

        String filePath = Paths.get(workingDirectory, tokenSymbol + "pdmaga.csv").toString();
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header
            writer.write("Open.time,High,Low,Close,SMA,Fast Avg Gain/Loss,Slow Avg Gain/Loss,Order Signal,Holdings\n");

            String[] openTime = originalData.getOpenTime();
            double[] high = originalData.getHigh();
            double[] low = originalData.getLow();
            double[] close = originalData.getClose();

            Map<String, double[]> data = tokenData.getData();
            double[] sma = data.get("SMA");
            double[] fast = data.get("Fast Avg Gain/Loss");
            double[] slow = data.get("Slow Avg Gain/Loss");
            double[] orderSignal = data.get("Order Signal");
            double[] holdings = data.get("Holdings");

            int length = openTime.length;

            for (int i = 0; i < length; i++) {
                writer.write(String.format("%s,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.1f,%.1f\n",
                        openTime[i],
                        high[i],
                        low[i],
                        close[i],
                        sma[i],
                        fast[i],
                        slow[i],
                        orderSignal[i],
                        holdings[i]));
            }
        }
    }

    // Begin Agentic Workflow Section - 5. execute standard trading rules
    public static double executeStandardTradeRules(String type) {
        System.out.println("Generating signals");
        System.out.println("1: " + LocalDateTime.now());
        TokenData tokenData = generateCustomSignals(lookbackPeriod, breakoutThresholdPct);
        System.out.println("2: " + LocalDateTime.now());
        System.out.println("Custom signals complete\n");

        System.out.println("Calculating profit");
        System.out.println("3: " + LocalDateTime.now());
        int fromPos = lookbackPeriod;
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
    public static TokenData generateCustomSignals(int lookbackPeriod, double breakoutThresholdPct) {
        TokenData tokenData = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        double[] close = ORIGINAL_DATA.getClose();
        int n = close.length;

        double[] rollingHigh = new double[n];
        double[] rollingLow = new double[n];
        double[] orderSignal = new double[n];

        Arrays.fill(rollingHigh, 0.0);
        Arrays.fill(rollingLow, 0.0);
        Arrays.fill(orderSignal, 0.0);

        int start = Math.max(1, lookbackPeriod);
        double thr = breakoutThresholdPct / 100.0;

        // Simple breakout:
        // BUY  when close crosses above rollingHigh*(1+thr)
        // SELL when close crosses below rollingLow*(1-thr)
        for (int i = start; i < n; i++) {
            double high = Double.NEGATIVE_INFINITY;
            double low = Double.POSITIVE_INFINITY;

            int windowStart = Math.max(0, i - lookbackPeriod);
            for (int j = windowStart; j < i; j++) {
                double c = close[j];
                if (c > high) high = c;
                if (c < low) low = c;
            }

            rollingHigh[i] = (high == Double.NEGATIVE_INFINITY) ? 0.0 : high;
            rollingLow[i] = (low == Double.POSITIVE_INFINITY) ? 0.0 : low;

            if (i > 0 && rollingHigh[i] > 0.0 && rollingLow[i] > 0.0) {
                double prev = close[i - 1];
                double cur = close[i];

                double buyLevel = rollingHigh[i] * (1.0 + thr);
                double sellLevel = rollingLow[i] * (1.0 - thr);

                boolean buyCross = prev <= buyLevel && cur > buyLevel;
                boolean sellCross = prev >= sellLevel && cur < sellLevel;

                if (buyCross) orderSignal[i] = BUY;
                else if (sellCross) orderSignal[i] = SELL;
            }
        }

        tokenData.getData().put("Rolling High", rollingHigh);
        tokenData.getData().put("Rolling Low", rollingLow);
        tokenData.getData().put("Order Signal", orderSignal);

        return tokenData;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    // Begin Agentic Workflow Section - 7. calculate profit
    public static double calculateProfit(int fromPos, int toPos, TokenData tokenData) {
        double[] holdingsVector = new double[ORIGINAL_DATA.getClose().length];
        Arrays.fill(holdingsVector, 0.0);

        double currentHoldings = INITIAL_INVESTMENT;
        double purchasePrice = 0.0;
        boolean inPosition = false;
        double tokenAmount = 0.0;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = ORIGINAL_DATA.getClose();

        for (int i = Math.max(0, fromPos); i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) {
                continue;
            }

            if (orderSignal[i] == BUY && !inPosition) {
                purchasePrice = closePrice[i];

                // fee on buy
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;

                tokenAmount = currentHoldings / purchasePrice;
                inPosition = true;
                holdingsVector[i] = currentHoldings;

            } else if (orderSignal[i] == SELL && inPosition) {
                double sellValue = tokenAmount * closePrice[i];

                // fee on sell
                double fee = FEE_RATE * sellValue;
                sellValue -= fee;

                currentHoldings = sellValue;
                inPosition = false;
                tokenAmount = 0.0;
                purchasePrice = 0.0;
                holdingsVector[i] = currentHoldings;

            } else {
                holdingsVector[i] = inPosition ? tokenAmount * closePrice[i] : currentHoldings;
            }
        }

        tokenData.getData().put("Holdings", holdingsVector);

        // If still in position at end, mark-to-market (already reflected in holdingsVector)
        double finalHoldings = inPosition ? tokenAmount * closePrice[toPos] : currentHoldings;
        return finalHoldings - INITIAL_INVESTMENT;
    }
    // End Agentic Workflow Section - 7. calculate profit

    public static double calculateCompoundedReturnRate(int fromPos, int toPos, TokenData tokenData) {
        double compoundedReturnRate = 0.0;
        boolean holdFlag = false;
        double purchasePrice = 0.0;
        double currentHoldings = INITIAL_INVESTMENT;
        double holdTokenAmount = 0.0;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = (double[]) ORIGINAL_DATA.getClose();

        for (int i = fromPos + 1; i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) {
                continue;
            }

            if (orderSignal[i] == 1 && !holdFlag) {
                purchasePrice = closePrice[i];

                // Deduct fee on purchase
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;

                // Buy tokens
                holdTokenAmount = currentHoldings / purchasePrice;
                holdFlag = true;

            } else if (orderSignal[i] == -1 && holdFlag) {
                double sellPrice = closePrice[i];

                // Sell tokens
                double holdingsAfterGain = holdTokenAmount * sellPrice;

                // Deduct fee on sale
                double fee = FEE_RATE * holdingsAfterGain;
                double finalHoldings = holdingsAfterGain - fee;

                double logReturn = Math.log(finalHoldings / currentHoldings); // relative to previous trade
                compoundedReturnRate += logReturn * 100.0;

                // Calculate percentage gain/loss over original (pre-trade) holdings
//                double netReturn = ((finalHoldings - INITIAL_INVESTMENT) / INITIAL_INVESTMENT) * 100.0;
//                if(netReturn < 0) {
//                    System.out.println("netReturn: " + netReturn);
//                }
//                aggregateReturnRate += netReturn; // regular ARR
//                aggregateReturnRate += (netReturn * linearScale[i]); // Linear Scale
//                aggregateReturnRate += (netReturn < 0) ? netReturn * 15 : netReturn; // Basic Penalty
//                aggregateReturnRate += (netReturn < 0)
//                        ? -1.5 * Math.pow(Math.abs(netReturn), 1.2)
//                        : netReturn; // Asymmetric Weighting (Balanced Penalty Multiplier)
//                aggregateReturnRate += (netReturn < 0)
//                        ? -2.0 * Math.pow(Math.abs(netReturn), 1.5)
//                        : netReturn; // // Asymmetric Weighting (Strong Penalty Multiplier)
//                aggregateReturnRate += (netReturn < 0)
//                        ? -3.0 * Math.pow(Math.abs(netReturn), 2.0)
//                        : netReturn; // // Asymmetric Weighting (Ultra-Aggressive Penalty Multiplier)

                // Reset for next trade
                currentHoldings = finalHoldings;
                holdFlag = false;
            }
        }

//        if(aggregateReturnRate < 0)
//            System.out.println("aggregateReturnRate: " + aggregateReturnRate);

        return compoundedReturnRate;
    }

    public static double[] generateLinearScale(double begin, double end, int length) {
        if (length < 2) {
            throw new IllegalArgumentException("Length must be at least 2 to include both start and end values.");
        }

        double[] scale = new double[length];
        double step = (end - begin) / (length - 1);

        for (int i = 0; i < length; i++) {
            scale[i] = begin + i * step;
        }

        return scale;
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

    /**
     * Genetic Algorithm Code
     */

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

        // lookbackPeriod
        intToBits(seed,
                bitPositions.get("LOOKBACK_PERIOD").getStartPos(),
                bitPositions.get("LOOKBACK_PERIOD").getEndPos(),
                lookbackPeriod);

        // breakoutThresholdPct (% stored as raw = pct * INDICATOR_DIVIDER)
        int rawThr = (int) Math.round(breakoutThresholdPct * INDICATOR_DIVIDER);
        intToBits(seed,
                bitPositions.get("BREAKOUT_THRESHOLD").getStartPos(),
                bitPositions.get("BREAKOUT_THRESHOLD").getEndPos(),
                rawThr);

        // Debug
        System.out.println("SEED DEBUG:");
        System.out.printf("Input lookbackPeriod = %d%n", lookbackPeriod);
        System.out.printf("Input breakoutThresholdPct = %.3f%%, raw = %d, decoded = %.3f%%%n",
                breakoutThresholdPct, rawThr, rawThr / (double) INDICATOR_DIVIDER);

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
        public FitnessFunction() {
        }

        @Override
        public double getFitness(BitString candidate, List<? extends BitString> population) {
            int lookback = bitsToInt(candidate,
                    bitPositions.get("LOOKBACK_PERIOD").getStartPos(),
                    bitPositions.get("LOOKBACK_PERIOD").getEndPos());

            int rawThr = bitsToInt(candidate,
                    bitPositions.get("BREAKOUT_THRESHOLD").getStartPos(),
                    bitPositions.get("BREAKOUT_THRESHOLD").getEndPos());
            double thresholdPct = rawThr / (double) INDICATOR_DIVIDER;

            // Guards: keep window meaningful and ensure we have enough data.
            // Threshold can be 0 (pure breakout) and is inherently non-negative by encoding.
            if (lookback < 2 || lookback >= ORIGINAL_DATA.getClose().length) {
                return 0;
            }

            TokenData tokenData = generateCustomSignals(lookback, thresholdPct);
            int fromPos = lookback;
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
            return population; // Skip crossover and return the population unchanged
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
                stagnantGenerations = 0; // Reset stagnation count
            } else {
                stagnantGenerations++; // Increment stagnation count
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

            int lookback = bitsToInt(bestCandidate,
                    bitPositions.get("LOOKBACK_PERIOD").getStartPos(),
                    bitPositions.get("LOOKBACK_PERIOD").getEndPos());

            int rawThr = bitsToInt(bestCandidate,
                    bitPositions.get("BREAKOUT_THRESHOLD").getStartPos(),
                    bitPositions.get("BREAKOUT_THRESHOLD").getEndPos());
            double thresholdPct = rawThr / (double) INDICATOR_DIVIDER;

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            String formattedDate = now.format(formatter);

            System.out.printf(
                    "Generation %d: Best fitness: %.3f, Solution (lookback, thresholdPct%%): %d, %.2f%% - %s%n",
                    data.getGenerationNumber(),
                    data.getBestCandidateFitness(),
                    lookback,
                    thresholdPct,
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

            return new BitPositions(m, cursor); // cursor == total bit width
        }

        public BitField get(String key) {
            return fields.get(key);
        }

        public int getBitWidth() {
            return bitWidth;
        }
    }
}
