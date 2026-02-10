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

public class AlgoV26 {
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
    // No hard fixed variables for this variation.
    // Risk/exit behaviour is encoded as GA-searchable parameters.
    // End Agentic Workflow Section - 0. fixed variables

    // Begin Agentic Workflow Section - 1. input parameters
    // Variation params (EMA crossover + RSI filter + trailing stop + take-profit)
    static int fastEmaPeriod;
    static int slowEmaPeriod;
    static int rsiPeriod;
    static double rsiBuyMax;   // buy only if RSI <= this
    static double rsiSellMin;  // sell if RSI >= this
    static double trailingStopPct;
    static double takeProfitPct;
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
            // Periods (minute-bars)
            "FAST_EMA", 7,          // 0..127
            "SLOW_EMA", 8,          // 0..255
            "RSI_PERIOD", 6,        // 0..63

            // Thresholds / percents (scaled by INDICATOR_DIVIDER)
            "RSI_BUY_MAX", 7,       // 0..127 -> 0..127
            "RSI_SELL_MIN", 7,      // 0..127 -> 0..127
            "TRAIL_STOP", 7,        // 0..127 -> 0..0.127
            "TAKE_PROFIT", 7        // 0..127 -> 0..0.127
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
        fastEmaPeriod = 20;
        slowEmaPeriod = 80;
        rsiPeriod = 14;
        rsiBuyMax = 40;
        rsiSellMin = 65;
        trailingStopPct = 0.030;
        takeProfitPct = 0.080;

        // GA_SEED switch (uses search space params plugged in above)
        GA_SEED = false;
        FITNESS_METHOD = FitnessMethod.X_OVER_HODL;

        // Other
        INDICATOR_DIVIDER = 1000;
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

        fastEmaPeriod = bitsToInt(bestSolution, bitPositions.get("FAST_EMA").getStartPos(), bitPositions.get("FAST_EMA").getEndPos());
        slowEmaPeriod = bitsToInt(bestSolution, bitPositions.get("SLOW_EMA").getStartPos(), bitPositions.get("SLOW_EMA").getEndPos());
        rsiPeriod = bitsToInt(bestSolution, bitPositions.get("RSI_PERIOD").getStartPos(), bitPositions.get("RSI_PERIOD").getEndPos());

        rsiBuyMax = bitsToInt(bestSolution, bitPositions.get("RSI_BUY_MAX").getStartPos(), bitPositions.get("RSI_BUY_MAX").getEndPos());
        rsiSellMin = bitsToInt(bestSolution, bitPositions.get("RSI_SELL_MIN").getStartPos(), bitPositions.get("RSI_SELL_MIN").getEndPos());
        trailingStopPct = bitsToInt(bestSolution, bitPositions.get("TRAIL_STOP").getStartPos(), bitPositions.get("TRAIL_STOP").getEndPos()) / (double) INDICATOR_DIVIDER;
        takeProfitPct = bitsToInt(bestSolution, bitPositions.get("TAKE_PROFIT").getStartPos(), bitPositions.get("TAKE_PROFIT").getEndPos()) / (double) INDICATOR_DIVIDER;

        System.out.printf(
                "Best solution (fastEma, slowEma, rsiPeriod, rsiBuyMax, rsiSellMin, trailStop, takeProfit): %d, %d, %d, %.0f, %.0f, %.3f, %.3f%n",
                fastEmaPeriod,
                slowEmaPeriod,
                rsiPeriod,
                rsiBuyMax,
                rsiSellMin,
                trailingStopPct,
                takeProfitPct
        );

        System.out.printf("GA execution time: %d milliseconds%n", (endTime - startTime));

        double profit = executeStandardTradeRules("GA");
        int warmup = Math.max(slowEmaPeriod, rsiPeriod) + 2;
        int fromPos = warmup;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos - 1, toPos);
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

        // Candidate params
        fastEmaPeriod = 20;
        slowEmaPeriod = 80;
        rsiPeriod = 14;
        rsiBuyMax = 40;
        rsiSellMin = 65;
        trailingStopPct = 0.030;
        takeProfitPct = 0.080;

        System.out.println("Begin Data Fetch: " + FROM_DATE);
        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        System.out.println("End Data Fetch: " + TO_DATE);
        System.out.println(ORIGINAL_DATA.getTokenSymbol());

        double profit = executeStandardTradeRules("SCE");
        int warmup = Math.max(slowEmaPeriod, rsiPeriod) + 2;
        int fromPos = warmup;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos - 1, toPos);
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
        TokenData tokenData = generateCustomSignals(
                fastEmaPeriod,
                slowEmaPeriod,
                rsiPeriod,
                rsiBuyMax,
                rsiSellMin,
                trailingStopPct,
                takeProfitPct
        );
        System.out.println("2: " + LocalDateTime.now());
        System.out.println("Custom signals complete\n");

        System.out.println("Calculating profit");
        System.out.println("3: " + LocalDateTime.now());
        int warmup = Math.max(slowEmaPeriod, rsiPeriod) + 2;
        int fromPos = warmup;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double profit = calculateProfit(fromPos - 1, toPos, tokenData);
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
    public static TokenData generateCustomSignals(
            int fastEmaPeriod,
            int slowEmaPeriod,
            int rsiPeriod,
            double rsiBuyMax,
            double rsiSellMin,
            double trailingStopPct,
            double takeProfitPct
    ) {
        TokenData tokenData = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        tokenData = updateEMA("Fast EMA", fastEmaPeriod, ORIGINAL_DATA.getClose(), tokenData);
        tokenData = updateEMA("Slow EMA", slowEmaPeriod, ORIGINAL_DATA.getClose(), tokenData);
        tokenData = updateRSI(rsiPeriod, ORIGINAL_DATA.getClose(), tokenData);

        tokenData = updateOrderSignalV2(
                slowEmaPeriod + rsiPeriod + 2,
                ORIGINAL_DATA.getClose(),
                tokenData,
                rsiBuyMax,
                rsiSellMin,
                trailingStopPct,
                takeProfitPct
        );

        return tokenData;
    }

    /**
     * Exponential Moving Average.
     * Writes vector under the provided key.
     */
    public static TokenData updateEMA(String key, int period, double[] closePrices, TokenData tokenData) {
        int n = closePrices.length;
        double[] ema = new double[n];
        if (n == 0) {
            tokenData.getData().put(key, ema);
            return tokenData;
        }

        // Seed EMA with first close to avoid a long warmup dependency.
        ema[0] = closePrices[0];
        double alpha = 2.0 / (period + 1.0);

        for (int i = 1; i < n; i++) {
            ema[i] = alpha * closePrices[i] + (1.0 - alpha) * ema[i - 1];
        }

        tokenData.getData().put(key, ema);
        return tokenData;
    }

    /**
     * RSI (Wilder's smoothing). Values in [0,100].
     */
    public static TokenData updateRSI(int period, double[] closePrices, TokenData tokenData) {
        int n = closePrices.length;
        double[] rsi = new double[n];
        Arrays.fill(rsi, 0.0);

        if (n < period + 1) {
            tokenData.getData().put("RSI", rsi);
            return tokenData;
        }

        double gainSum = 0.0;
        double lossSum = 0.0;

        for (int i = 1; i <= period; i++) {
            double diff = closePrices[i] - closePrices[i - 1];
            if (diff >= 0) gainSum += diff;
            else lossSum += -diff;
        }

        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;

        rsi[period] = (avgLoss == 0.0) ? 100.0 : (100.0 - (100.0 / (1.0 + (avgGain / avgLoss))));

        for (int i = period + 1; i < n; i++) {
            double diff = closePrices[i] - closePrices[i - 1];
            double gain = Math.max(diff, 0.0);
            double loss = Math.max(-diff, 0.0);

            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;

            if (avgLoss == 0.0) {
                rsi[i] = 100.0;
            } else {
                double rs = avgGain / avgLoss;
                rsi[i] = 100.0 - (100.0 / (1.0 + rs));
            }
        }

        tokenData.getData().put("RSI", rsi);
        return tokenData;
    }

    /**
     * Variation replacement for updateOrderSignal():
     * - Entry: fast EMA crosses above slow EMA AND RSI <= rsiBuyMax
     * - Exit: fast EMA crosses below slow EMA OR RSI >= rsiSellMin OR trailing-stop OR take-profit
     */
    public static TokenData updateOrderSignalV2(
            int startPos,
            double[] closePrices,
            TokenData tokenData,
            double rsiBuyMax,
            double rsiSellMin,
            double trailingStopPct,
            double takeProfitPct
    ) {
        int n = closePrices.length;
        double[] orderSignal = new double[n];
        Arrays.fill(orderSignal, 0.0);

        final int BUY = 1;
        final int SELL = -1;
        final int HOLD = 0;

        double[] fastEma = tokenData.getData().get("Fast EMA");
        double[] slowEma = tokenData.getData().get("Slow EMA");
        double[] rsi = tokenData.getData().get("RSI");

        boolean inPosition = false;
        double entryPrice = 0.0;
        double peakPrice = 0.0;

        for (int i = Math.max(1, startPos); i < n; i++) {
            double close = closePrices[i];

            boolean crossUp = fastEma[i - 1] <= slowEma[i - 1] && fastEma[i] > slowEma[i];
            boolean crossDown = fastEma[i - 1] >= slowEma[i - 1] && fastEma[i] < slowEma[i];

            boolean buyCondition = !inPosition && crossUp && rsi[i] <= rsiBuyMax;

            boolean trailStopHit = inPosition && trailingStopPct > 0.0 && close <= peakPrice * (1.0 - trailingStopPct);
            boolean takeProfitHit = inPosition && takeProfitPct > 0.0 && close >= entryPrice * (1.0 + takeProfitPct);
            boolean sellCondition = inPosition && (crossDown || rsi[i] >= rsiSellMin || trailStopHit || takeProfitHit);

            if (buyCondition) {
                orderSignal[i] = BUY;
                inPosition = true;
                entryPrice = close;
                peakPrice = close;
            } else if (sellCondition) {
                orderSignal[i] = SELL;
                inPosition = false;
                entryPrice = 0.0;
                peakPrice = 0.0;
            } else {
                orderSignal[i] = HOLD;
                if (inPosition && close > peakPrice) {
                    peakPrice = close;
                }
            }
        }

        tokenData.getData().put("Order Signal", orderSignal);
        return tokenData;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    // Begin Agentic Workflow Section - 7. calculate profit
    public static double calculateProfit(int fromPos, int toPos, TokenData tokenData) {
        double[] holdingsVector = new double[ORIGINAL_DATA.getClose().length];
        Arrays.fill(holdingsVector, 0.0);

        double currentHoldingsUsd = INITIAL_INVESTMENT;
        double positionTokens = 0.0;
        boolean inPosition = false;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = ORIGINAL_DATA.getClose();

        for (int i = fromPos + 1; i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) continue;

            if (orderSignal[i] == BUY && !inPosition) {
                // Buy with all USD
                double fee = FEE_RATE * currentHoldingsUsd;
                currentHoldingsUsd -= fee;
                positionTokens = currentHoldingsUsd / closePrice[i];
                inPosition = true;
                holdingsVector[i] = currentHoldingsUsd;

            } else if (orderSignal[i] == SELL && inPosition) {
                // Sell all tokens into USD
                double grossUsd = positionTokens * closePrice[i];
                double fee = FEE_RATE * grossUsd;
                currentHoldingsUsd = grossUsd - fee;
                positionTokens = 0.0;
                inPosition = false;
                holdingsVector[i] = currentHoldingsUsd;

            } else {
                // Mark-to-market
                holdingsVector[i] = inPosition ? (positionTokens * closePrice[i]) : currentHoldingsUsd;
            }
        }

        tokenData.getData().put("Holdings", holdingsVector);

        double finalHoldings = inPosition ? (positionTokens * closePrice[toPos]) : currentHoldingsUsd;
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

        intToBits(seed, bitPositions.get("FAST_EMA").getStartPos(), bitPositions.get("FAST_EMA").getEndPos(), fastEmaPeriod);
        intToBits(seed, bitPositions.get("SLOW_EMA").getStartPos(), bitPositions.get("SLOW_EMA").getEndPos(), slowEmaPeriod);
        intToBits(seed, bitPositions.get("RSI_PERIOD").getStartPos(), bitPositions.get("RSI_PERIOD").getEndPos(), rsiPeriod);

        intToBits(seed, bitPositions.get("RSI_BUY_MAX").getStartPos(), bitPositions.get("RSI_BUY_MAX").getEndPos(), (int) Math.round(rsiBuyMax));
        intToBits(seed, bitPositions.get("RSI_SELL_MIN").getStartPos(), bitPositions.get("RSI_SELL_MIN").getEndPos(), (int) Math.round(rsiSellMin));
        intToBits(seed, bitPositions.get("TRAIL_STOP").getStartPos(), bitPositions.get("TRAIL_STOP").getEndPos(), (int) Math.round(trailingStopPct * INDICATOR_DIVIDER));
        intToBits(seed, bitPositions.get("TAKE_PROFIT").getStartPos(), bitPositions.get("TAKE_PROFIT").getEndPos(), (int) Math.round(takeProfitPct * INDICATOR_DIVIDER));

        System.out.println("SEED DEBUG:");
        System.out.printf("fastEma=%d slowEma=%d rsiPeriod=%d rsiBuyMax=%.0f rsiSellMin=%.0f trailStop=%.3f takeProfit=%.3f%n",
                fastEmaPeriod, slowEmaPeriod, rsiPeriod, rsiBuyMax, rsiSellMin, trailingStopPct, takeProfitPct);

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
            int fastEmaPeriod = bitsToInt(candidate,
                    bitPositions.get("FAST_EMA").getStartPos(),
                    bitPositions.get("FAST_EMA").getEndPos());
            int slowEmaPeriod = bitsToInt(candidate,
                    bitPositions.get("SLOW_EMA").getStartPos(),
                    bitPositions.get("SLOW_EMA").getEndPos());
            int rsiPeriod = bitsToInt(candidate,
                    bitPositions.get("RSI_PERIOD").getStartPos(),
                    bitPositions.get("RSI_PERIOD").getEndPos());

            double rsiBuyMax = bitsToInt(candidate,
                    bitPositions.get("RSI_BUY_MAX").getStartPos(),
                    bitPositions.get("RSI_BUY_MAX").getEndPos());
            double rsiSellMin = bitsToInt(candidate,
                    bitPositions.get("RSI_SELL_MIN").getStartPos(),
                    bitPositions.get("RSI_SELL_MIN").getEndPos());

            double trailingStopPct = bitsToInt(candidate,
                    bitPositions.get("TRAIL_STOP").getStartPos(),
                    bitPositions.get("TRAIL_STOP").getEndPos()) / (double) INDICATOR_DIVIDER;
            double takeProfitPct = bitsToInt(candidate,
                    bitPositions.get("TAKE_PROFIT").getStartPos(),
                    bitPositions.get("TAKE_PROFIT").getEndPos()) / (double) INDICATOR_DIVIDER;

            // Minimal guards: avoid zero-length indicators and inverted EMA crossover.
            if (fastEmaPeriod < 2 || slowEmaPeriod < 3 || rsiPeriod < 2) return 0;
            if (fastEmaPeriod >= slowEmaPeriod) return 0;

            // RSI thresholds should be sane.
            if (rsiBuyMax < 1 || rsiBuyMax > 80) return 0;
            if (rsiSellMin < 20 || rsiSellMin > 99) return 0;
            if (rsiBuyMax >= rsiSellMin) return 0;

            // Ensure we have enough history.
            int warmup = Math.max(slowEmaPeriod, rsiPeriod) + 2;
            if (warmup + 2 >= ORIGINAL_DATA.getClose().length) return 0;

            TokenData tokenData = generateCustomSignals(
                    fastEmaPeriod,
                    slowEmaPeriod,
                    rsiPeriod,
                    rsiBuyMax,
                    rsiSellMin,
                    trailingStopPct,
                    takeProfitPct
            );

            int fromPos = warmup;
            int toPos = ORIGINAL_DATA.getClose().length - 1;

            double profit = calculateProfit(fromPos - 1, toPos, tokenData);
            double hodlProfit = executeStandardModel(fromPos - 1, toPos);

            double fitnessScore = 0;
            if (FITNESS_METHOD == FitnessMethod.X_OVER_HODL) fitnessScore = profit / hodlProfit;
            if (FITNESS_METHOD == FitnessMethod.PROFIT) fitnessScore = profit;
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

            int fastEmaPeriod = bitsToInt(bestCandidate,
                    bitPositions.get("FAST_EMA").getStartPos(),
                    bitPositions.get("FAST_EMA").getEndPos());
            int slowEmaPeriod = bitsToInt(bestCandidate,
                    bitPositions.get("SLOW_EMA").getStartPos(),
                    bitPositions.get("SLOW_EMA").getEndPos());
            int rsiPeriod = bitsToInt(bestCandidate,
                    bitPositions.get("RSI_PERIOD").getStartPos(),
                    bitPositions.get("RSI_PERIOD").getEndPos());

            double rsiBuyMax = bitsToInt(bestCandidate,
                    bitPositions.get("RSI_BUY_MAX").getStartPos(),
                    bitPositions.get("RSI_BUY_MAX").getEndPos());
            double rsiSellMin = bitsToInt(bestCandidate,
                    bitPositions.get("RSI_SELL_MIN").getStartPos(),
                    bitPositions.get("RSI_SELL_MIN").getEndPos());

            double trailingStopPct = bitsToInt(bestCandidate,
                    bitPositions.get("TRAIL_STOP").getStartPos(),
                    bitPositions.get("TRAIL_STOP").getEndPos()) / (double) INDICATOR_DIVIDER;
            double takeProfitPct = bitsToInt(bestCandidate,
                    bitPositions.get("TAKE_PROFIT").getStartPos(),
                    bitPositions.get("TAKE_PROFIT").getEndPos()) / (double) INDICATOR_DIVIDER;

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            String formattedDate = now.format(formatter);

            System.out.printf(
                    "Generation %d: Best fitness: %.3f, Solution (fastEma, slowEma, rsiPeriod, rsiBuyMax, rsiSellMin, trailStop, takeProfit): %d, %d, %d, %.0f, %.0f, %.3f, %.3f - %s%n",
                    data.getGenerationNumber(),
                    data.getBestCandidateFitness(),
                    fastEmaPeriod,
                    slowEmaPeriod,
                    rsiPeriod,
                    rsiBuyMax,
                    rsiSellMin,
                    trailingStopPct,
                    takeProfitPct,
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
