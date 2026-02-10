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

public class BaseAlgo {
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
            "AVG_LOSS_MIN", 6,
            "AVG_GAIN_MIN", 6,
            "PERIOD1", 10,
            "PERIOD2", 10,
            "PERIOD3", 10
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
//            WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\kraken";
        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\binance";
        DATA_RESOLUTION = 15;
        FEE_RATE = 0.001;
        FROM_DATE = "2020-07-24";
        TO_DATE = "2025-07-30";
        // Objective Function Search Space Params
        avgLossMin = 0.000;
        avgGainMin = -0.017;
        period1 = 944;
        period2 = 194;
        period3 = 516;
        // Objective Function Hard Fixed Params
        STOP_LOSS = 0.0160;
        STOP_LOSS2 = 0.985;
        STOP_LOSS3 = 0.914;

        // GA_SEED switch (uses search space params plugged in above)
        GA_SEED = false;
        // Set fitness type;
        FITNESS_METHOD = FitnessMethod.X_OVER_HODL;

        // Other
        INDICATOR_DIVIDER = 1000;
        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);

//            linearScale = generateLinearScale(1, 10, ORIGINAL_DATA.getClose().length);

        CandidateFactory<BitString> candidateFactory = new BitStringFactory(bitPositions.getBitWidth());
        EvolutionaryOperator<BitString> pipeline = new EvolutionPipeline<>(Arrays.asList(
                new ProbabilisticCrossover(1, new Probability(CROSSOVER_PROBABILITY)), // Single-point crossover with probability
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

        int rawValue = bitsToInt(bestSolution, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos());
        int bitLength = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos(); // ✅ fixed
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
        /**
         * 1st
         */
        TOKEN = "BTC";
//            WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\kraken";
        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\binance";
        DATA_RESOLUTION = 15;
        FEE_RATE = 0.001;
        FROM_DATE = "2020-07-24";
        TO_DATE = "2025-07-30";
        // Objective Function Search Space Params
        avgLossMin = 0.000;
        avgGainMin = -0.029;
        period1 = 987;
        period2 = 162;
        period3 = 375;
        // Objective Function Hard Fixed Params
        STOP_LOSS = 0.0160;
        STOP_LOSS2 = 0.985;
        STOP_LOSS3 = 0.914;

        System.out.println("Begin Data Fetch: " + FROM_DATE);
        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        System.out.println("End Data Fetch: " + TO_DATE);
        System.out.println(ORIGINAL_DATA.getTokenSymbol());
        double profit = executeStandardTradeRules("SCE");
        int fromPos = (period1 + period3);
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
        // Generate signals
        System.out.println("1: " + LocalDateTime.now());
        TokenData tokenData = generateCustomSignals(avgLossMin, avgGainMin, period1, period2, period3);
        System.out.println("2: " + LocalDateTime.now());
        System.out.println("Custom signals complete\n");

        System.out.println("Calculating profit");
        // Calculate profit
        System.out.println("3: " + LocalDateTime.now());
        int fromPos = (period1 + period3);
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
    public static TokenData generateCustomSignals(double avgLossMin, double avgGainMin, int period1, int period2, int period3) {
        TokenData tokenData = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        tokenData = updateSMA(period1, ORIGINAL_DATA.getClose(), tokenData);

        tokenData = updateFastAverageGainLoss(
                period1 + period2 - 1,
                period2,
                tokenData.getData().get("SMA"),
                tokenData
        );

        tokenData = updateSlowAverageGainLoss(
                period1 + period3 - 1,
                period3,
                tokenData.getData().get("SMA"),
                tokenData
        );

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

        // Rolling sum to compute the SMA incrementally
        double rollingSum = 0.0;

        for (int i = 0; i < dataLength; i++) {
            if (i < smaPeriod - 1) {
                // Fill with 0 for indices before the SMA period is reached
                smaVector[i] = 0.0;
                rollingSum += closePrices[i]; // Accumulate until we reach the full period
            } else {
                // Add the current price to the rolling sum
                rollingSum += closePrices[i];

                // Calculate SMA
                smaVector[i] = rollingSum / smaPeriod;

                // Remove the price that is no longer in the window
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

                if (current != 0.0) {
                    fastAverageGainLossVector[i] = gain;
                } else {
                    fastAverageGainLossVector[i] = 0.0;
                }
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
                if (current != 0.0) {
                    slowAverageGainLossVector[i] = ((current - previous) / period) / current * 100.0;
                } else {
                    slowAverageGainLossVector[i] = 0.0;
                }
            }
        }

        tokenData.getData().put("Slow Avg Gain/Loss", slowAverageGainLossVector);

        return tokenData;
    }

    public static TokenData updateOrderSignal(double avgLossMin, double avgGainMin, int period, double[] closePrices, TokenData tokenData, double stopLoss) {
        int dataLength = closePrices.length;
        double[] orderSignalVector = new double[dataLength];

        final int BUY = 1;
        final int SELL = -1;
        final int HOLD = 0;

        double buySignalLastPrice = 0.0;
        double currentPeak = 0.0;
        int dangerCount = 0;
        int periodSinceBuy = 0;
        double purchasePrice = 0.0;

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
                            (close / currentSMA < 0.69);

            boolean sellCondition =
                    fast < avgGainMin && slow > fast;

            if (buyCondition) {
                orderSignalVector[i] = BUY;
                buySignalLastPrice = close;

                if (close > currentPeak)
                    currentPeak = close;

                if (purchasePrice == 0.0) {
                    purchasePrice = close;
                    periodSinceBuy = 0;
                }

                periodSinceBuy++;
                dangerCount = 0;

            } else if (sellCondition) {
                orderSignalVector[i] = SELL;
                currentPeak = 0.0;
                dangerCount = 0;
                purchasePrice = 0.0;
                periodSinceBuy = 0;

            } else {
                orderSignalVector[i] = HOLD;
                periodSinceBuy++;

                if (i > 0 &&
                        purchasePrice != 0.0 &&
                        closePrices[i - 1] / purchasePrice >= STOP_LOSS2 &&
                        close / purchasePrice < STOP_LOSS2) {

                    orderSignalVector[i] = SELL;
                    purchasePrice = 0.0;
                    periodSinceBuy = 0;
                }
            }

            // R-style SELL block: partially guarded
            if (i > 0 &&
                    (
                            (purchasePrice != 0.0 &&
                                    closePrices[i - 1] / purchasePrice >= STOP_LOSS3 &&
                                    close / purchasePrice < STOP_LOSS3) ||
                                    (close < buySignalLastPrice * (1.0 - stopLoss)) ||
                                    (close / currentSMA > 1.28)
                    )
            ) {
                orderSignalVector[i] = SELL;
                purchasePrice = 0.0;
                periodSinceBuy = 0;
            }
        }

        tokenData.getData().put("Order Signal", orderSignalVector);
        return tokenData;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    public static double calculateProfit(int fromPos, int toPos, TokenData tokenData) {
        double[] holdingsVector = new double[ORIGINAL_DATA.getClose().length];
        Arrays.fill(holdingsVector, 0.0);

        double profit = 0.00;
        double currentHoldings = INITIAL_INVESTMENT;
        double profitAsPercentage = 0.00;
        double purchasePrice = 0.0;
        boolean holdFlag = false;
        double holdTokenAmount = 0.00;
        double profitGained = 0.00;
        int tradeCount = 0;
        int sellCount = 0;
        int winningTrades = 0;
        double feeTotal = 0.00;
        double peakHoldings = INITIAL_INVESTMENT;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = (double[]) ORIGINAL_DATA.getClose();
        String[] openTimes = ORIGINAL_DATA.getOpenTime();

        for (int i = fromPos + 1; i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) {
                continue;
            }

            if (orderSignal[i] == 1 && !holdFlag) {
//                System.out.println("Close null? " + (tokenData.getClose() == null));
                purchasePrice = closePrice[i];
                double fee = FEE_RATE * currentHoldings;
                feeTotal += fee;
                currentHoldings -= fee;
                holdFlag = true;
                holdTokenAmount = currentHoldings / purchasePrice;
                tradeCount++;
                holdingsVector[i] = currentHoldings;
//                System.out.printf("TOKEN BOUGHT - purchase price: %.2f, open price: %.2f on *** %s *** holdings: %.2f%n",
//                        purchasePrice, closePrice[i], openTimes[i], currentHoldings);

            } else if (orderSignal[i] == -1 && holdFlag) {
                profitAsPercentage = (100.0 / purchasePrice) * (closePrice[i] - purchasePrice);
                profitGained = (profitAsPercentage / 100.0) * currentHoldings;

                if (profitAsPercentage > 0) {
                    winningTrades++;
                }

                sellCount++;
                currentHoldings += profitGained;
                double fee = FEE_RATE * currentHoldings;
                feeTotal += fee;
                currentHoldings -= fee;
                holdFlag = false;
                tradeCount++;
                holdingsVector[i] = currentHoldings;

//                System.out.printf("TOKEN SOLD - profit gained: %.2f, as %%: %.2f, purchase price: %.2f, open price: %.2f on *** %s *** holdings: %.2f%n",
//                        profitGained, profitAsPercentage, purchasePrice, closePrice[i], openTimes[i], currentHoldings);
            } else {
                if (!holdFlag) {
                    holdingsVector[i] = currentHoldings;
                } else {
                    holdingsVector[i] = holdTokenAmount * closePrice[i];
                }
            }

            if (holdingsVector[i] > peakHoldings) {
                peakHoldings = holdingsVector[i];
            }
        }

        // Update tokenData
        tokenData.getData().put("Holdings", holdingsVector);

        // Compute profit
        profit = currentHoldings - INITIAL_INVESTMENT;

        // Print stats
//        System.out.printf("Trade count = %d%n", tradeCount);
        double winRate = (sellCount > 0) ? (100.0 * winningTrades / sellCount) : 0.0;
//        System.out.printf("Win Rate = %.2f%%%n", winRate);
//        System.out.printf("Fee total = %.2f%n", feeTotal);
        double lossFromPeak = ((currentHoldings / peakHoldings) - 1.0) * 100.0;
//        System.out.printf("Holdings Loss From Peak = %.2f%%%n", lossFromPeak);

        return profit;
    }

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

    // Begin Agentic Workflow Section - 7. generate seed candidates
    private static List<BitString> generateSeedCandidates() {
        BitString seed = new BitString(bitPositions.getBitWidth());

        // avgLossMin
        int lossBitLength = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos();
        double lossMid = Math.pow(2, lossBitLength) / 2.0;
        int rawLoss = (int) Math.round((avgLossMin * INDICATOR_DIVIDER) + lossMid);
        intToBits(seed, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos(), rawLoss);

        // avgGainMin
        int gainBitLength = bitPositions.get("AVG_GAIN_MIN").getEndPos() - bitPositions.get("AVG_GAIN_MIN").getStartPos();
        double gainMid = Math.pow(2, gainBitLength) / 2.0;
        int rawGain = (int) Math.round((avgGainMin * INDICATOR_DIVIDER) + gainMid);
        intToBits(seed, bitPositions.get("AVG_GAIN_MIN").getStartPos(), bitPositions.get("AVG_GAIN_MIN").getEndPos(), rawGain);

        // Periods
        intToBits(seed, bitPositions.get("PERIOD1").getStartPos(), bitPositions.get("PERIOD1").getEndPos(), period1);
        intToBits(seed, bitPositions.get("PERIOD2").getStartPos(), bitPositions.get("PERIOD2").getEndPos(), period2);
        intToBits(seed, bitPositions.get("PERIOD3").getStartPos(), bitPositions.get("PERIOD3").getEndPos(), period3);

        // Debug
        System.out.println("SEED DEBUG:");
        System.out.printf("Input avgLossMin = %.6f, raw = %d, decoded = %.6f%n",
                avgLossMin, rawLoss, (rawLoss - lossMid) / INDICATOR_DIVIDER);
        System.out.printf("Input avgGainMin = %.6f, raw = %d, decoded = %.6f%n",
                avgGainMin, rawGain, (rawGain - gainMid) / INDICATOR_DIVIDER);
        System.out.printf("Periods: %d, %d, %d%n", period1, period2, period3);

        return Collections.singletonList(seed);
    }
    // End Agentic Workflow Section - 7. generate seed candidates

    private static void intToBits(BitString bitString, int start, int end, int value) {
        for (int i = start; i < end; i++) {
            int bitIndex = i - start;
            boolean bit = ((value >> bitIndex) & 1) == 1;
            bitString.setBit(i, bit);
        }
    }

    // Begin Agentic Workflow Section - 8. fitness function
    private static class FitnessFunction implements FitnessEvaluator<BitString> {
        public FitnessFunction() {
        }

        @Override
        public double getFitness(BitString candidate, List<? extends BitString> population) {
            int rawValue = bitsToInt(candidate, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos());
            int bitLength = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos(); // ✅ fixed
            double midpoint = Math.pow(2, bitLength) / 2.0;
            double avgLossMin = (rawValue - midpoint) / INDICATOR_DIVIDER;

            int rawGainValue = bitsToInt(candidate, bitPositions.get("AVG_GAIN_MIN").getStartPos(), bitPositions.get("AVG_GAIN_MIN").getEndPos());
            int gainBitLength = bitPositions.get("AVG_GAIN_MIN").getEndPos() - bitPositions.get("AVG_GAIN_MIN").getStartPos();
            double gainMidpoint = Math.pow(2, gainBitLength) / 2.0;
            double avgGainMin = IS_AVG_GAIN_MIN_ONLY_POSITIVE
                    ? rawGainValue / INDICATOR_DIVIDER
                    : (rawGainValue - gainMidpoint) / INDICATOR_DIVIDER;

            int period1 = bitsToInt(candidate, bitPositions.get("PERIOD1").getStartPos(), bitPositions.get("PERIOD1").getEndPos());
            int period2 = bitsToInt(candidate, bitPositions.get("PERIOD2").getStartPos(), bitPositions.get("PERIOD2").getEndPos());
            int period3 = bitsToInt(candidate, bitPositions.get("PERIOD3").getStartPos(), bitPositions.get("PERIOD3").getEndPos());

            // Any guards to penalise algo or values we do not want to explore.
            if (period1 < 2 || period2 < 2 || period3 < 2 || period2 > period3 || (period1 + period3) > ORIGINAL_DATA.getClose().length) {
                return 0;
            }

            TokenData tokenData = BaseAlgo.generateCustomSignals(avgLossMin, avgGainMin, period1, period2, period3);
            int fromPos = (period1 + period3);
            int toPos = ORIGINAL_DATA.getClose().length - 1;

            double profit = BaseAlgo.calculateProfit(fromPos - 1, toPos, tokenData);
            double hodlProfit = BaseAlgo.executeStandardModel(fromPos - 1, toPos);

            /**
             * Toggle/Comment/Uncomment from the following x3 options to select fitness:
             *
             * In order of precedence:
             *
             * 1. x-over-hodl
             * 2. profit (if begin and end price of HODL is too close)
             * 3. compound return rate (more as an academic curiosity)
             *
             */

            double fitnessScore = 0;

            if (FITNESS_METHOD == FitnessMethod.X_OVER_HODL)
                fitnessScore = profit / hodlProfit;
            if (FITNESS_METHOD == FitnessMethod.PROFIT)
                fitnessScore = profit;
            if (FITNESS_METHOD == FitnessMethod.COMPOUND_RETURN_RATE)
                fitnessScore = calculateCompoundedReturnRate(fromPos, toPos, tokenData);

            return Math.max(fitnessScore, 0);  // Ensure non-negative fitness score
        }

        @Override
        public boolean isNatural() {
            return true;
        }
    }
    // End Agentic Workflow Section - 8. fitness function

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

    // Begin Agentic Workflow Section - 9. progress listener
    private static class ProgressListener implements EvolutionObserver<BitString> {
        private BitString bestCandidate;

        @Override
        public void populationUpdate(PopulationData<? extends BitString> data) {
            bestCandidate = data.getBestCandidate();

            int rawValue = bitsToInt(bestCandidate, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos());
            int bitLength = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos(); // ✅ fixed
            double midpoint = Math.pow(2, bitLength) / 2.0;
            double avgLossMin = (rawValue - midpoint) / INDICATOR_DIVIDER;

            int rawGainValue = bitsToInt(bestCandidate, bitPositions.get("AVG_GAIN_MIN").getStartPos(), bitPositions.get("AVG_GAIN_MIN").getEndPos());
            int gainBitLength = bitPositions.get("AVG_GAIN_MIN").getEndPos() - bitPositions.get("AVG_GAIN_MIN").getStartPos();
            double gainMidpoint = Math.pow(2, gainBitLength) / 2.0;
            double avgGainMin = IS_AVG_GAIN_MIN_ONLY_POSITIVE
                    ? rawGainValue / INDICATOR_DIVIDER
                    : (rawGainValue - gainMidpoint) / INDICATOR_DIVIDER;

            int period1 = bitsToInt(bestCandidate, bitPositions.get("PERIOD1").getStartPos(), bitPositions.get("PERIOD1").getEndPos());
            int period2 = bitsToInt(bestCandidate, bitPositions.get("PERIOD2").getStartPos(), bitPositions.get("PERIOD2").getEndPos());
            int period3 = bitsToInt(bestCandidate, bitPositions.get("PERIOD3").getStartPos(), bitPositions.get("PERIOD3").getEndPos());

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            String formattedDate = now.format(formatter);

            System.out.printf(
                    "Generation %d: Best fitness: %.3f, Solution (avgLossMin, avgGainMin, period1, period2, period3): %.3f, %.3f, %d, %d, %d - %s%n",
                    data.getGenerationNumber(),
                    data.getBestCandidateFitness(),
                    avgLossMin,
                    avgGainMin,
                    period1,
                    period2,
                    period3,
                    formattedDate
            );
        }

        public BitString getBestCandidate() {
            return bestCandidate;
        }
    }
    // End Agentic Workflow Section - 9. progress listener

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
