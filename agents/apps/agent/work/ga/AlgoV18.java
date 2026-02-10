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
 * AlgoV18: minimal variation from AlgoV16 with NO new parameters.
 * Small change: use Weighted Moving Average (WMA) instead of SMA for the baseline.
 * period1 is still the window length, and all GA params remain unchanged.
 */
public class AlgoV18 {
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
//        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\kraken";
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
//        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\kraken";
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
            writer.write("Open.time,High,Low,Close,WMA,Fast Avg Gain/Loss,Slow Avg Gain/Loss,Order Signal,Holdings\n");

            String[] openTime = originalData.getOpenTime();
            double[] high = originalData.getHigh();
            double[] low = originalData.getLow();
            double[] close = originalData.getClose();

            Map<String, double[]> data = tokenData.getData();
            double[] wma = data.get("WMA");
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
                        wma[i],
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
        TokenData tokenData = generateCustomSignals(avgLossMin, avgGainMin, period1, period2, period3);
        System.out.println("2: " + LocalDateTime.now());
        System.out.println("Custom signals complete\n");

        System.out.println("Calculating profit");
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

        tokenData = updateWMA(period1, ORIGINAL_DATA.getClose(), tokenData);

        tokenData = updateFastAverageGainLoss(
                period1 + period2 - 1,
                period2,
                tokenData.getData().get("WMA"),
                tokenData
        );

        tokenData = updateSlowAverageGainLoss(
                period1 + period3 - 1,
                period3,
                tokenData.getData().get("WMA"),
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

    /**
     * Small variation vs V16: WMA replaces SMA. Uses the same period1, no new params.
     * WMA weights recent prices more: weights 1..period.
     */
    public static TokenData updateWMA(int period1, double[] closePrices, TokenData tokenData) {
        int n = closePrices.length;
        double[] wma = new double[n];

        int period = Math.max(1, period1);
        int denom = period * (period + 1) / 2; // sum of weights 1..period

        for (int i = 0; i < n; i++) {
            if (i < period - 1) {
                wma[i] = 0.0;
                continue;
            }

            double weightedSum = 0.0;
            int w = 1;
            for (int j = i - period + 1; j <= i; j++) {
                weightedSum += closePrices[j] * w;
                w++;
            }
            wma[i] = weightedSum / denom;
        }

        tokenData.getData().put("WMA", wma);
        return tokenData;
    }

    public static TokenData updateFastAverageGainLoss(int startPos, int period, double[] baseValues, TokenData tokenData) {
        int dataLength = baseValues.length;
        double[] fastAverageGainLossVector = new double[dataLength];

        for (int i = 0; i < dataLength; i++) {
            if (i < startPos || i - period < 0) {
                fastAverageGainLossVector[i] = 0.0;
            } else {
                double current = baseValues[i];
                double previous = baseValues[i - period];
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

    public static TokenData updateSlowAverageGainLoss(int startPos, int period, double[] baseValues, TokenData tokenData) {
        int dataLength = baseValues.length;
        double[] slowAverageGainLossVector = new double[dataLength];

        for (int i = 0; i < dataLength; i++) {
            if (i < startPos || i - period < 0) {
                slowAverageGainLossVector[i] = 0.0;
            } else {
                double current = baseValues[i];
                double previous = baseValues[i - period];
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
        double purchasePrice = 0.0;

        double[] fastAvgGainLoss = tokenData.getData().get("Fast Avg Gain/Loss");
        double[] slowAvgGainLoss = tokenData.getData().get("Slow Avg Gain/Loss");
        double[] wma = tokenData.getData().get("WMA");

        for (int i = period; i < dataLength; i++) {
            double fast = fastAvgGainLoss[i];
            double slow = slowAvgGainLoss[i];
            double close = closePrices[i];
            double currentWma = wma[i];

            boolean buyCondition =
                    (fast < avgLossMin && slow < fast) ||
                            (currentWma != 0.0 && close / currentWma < 0.69);

            boolean sellCondition =
                    fast < avgGainMin && slow > fast;

            if (buyCondition) {
                orderSignalVector[i] = BUY;
                buySignalLastPrice = close;

                if (purchasePrice == 0.0) {
                    purchasePrice = close;
                }

            } else if (sellCondition) {
                orderSignalVector[i] = SELL;
                purchasePrice = 0.0;

            } else {
                orderSignalVector[i] = HOLD;

                if (i > 0 &&
                        purchasePrice != 0.0 &&
                        close < buySignalLastPrice * (1.0 - stopLoss)) {
                    orderSignalVector[i] = SELL;
                    purchasePrice = 0.0;
                }
            }

            if (i > 0 &&
                    (purchasePrice != 0.0 && currentWma != 0.0 && close / currentWma > 1.28)) {
                orderSignalVector[i] = SELL;
                purchasePrice = 0.0;
            }
        }

        tokenData.getData().put("Order Signal", orderSignalVector);
        return tokenData;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    // Begin Agentic Workflow Section - 7. calculate profit
    public static double calculateProfit(int fromPos, int toPos, TokenData tokenData) {
        double[] holdingsVector = new double[ORIGINAL_DATA.getClose().length];
        Arrays.fill(holdingsVector, 0.0);

        double currentHoldings = INITIAL_INVESTMENT;
        boolean holdFlag = false;
        double holdTokenAmount = 0.00;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = ORIGINAL_DATA.getClose();

        for (int i = fromPos + 1; i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) {
                continue;
            }

            if (orderSignal[i] == BUY && !holdFlag) {
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;
                holdFlag = true;
                holdTokenAmount = currentHoldings / closePrice[i];
                holdingsVector[i] = currentHoldings;

            } else if (orderSignal[i] == SELL && holdFlag) {
                currentHoldings = holdTokenAmount * closePrice[i];
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;
                holdFlag = false;
                holdingsVector[i] = currentHoldings;

            } else {
                holdingsVector[i] = holdFlag ? (holdTokenAmount * closePrice[i]) : currentHoldings;
            }
        }

        tokenData.getData().put("Holdings", holdingsVector);

        double finalHoldings = holdFlag ? (holdTokenAmount * closePrice[toPos]) : currentHoldings;
        return finalHoldings - INITIAL_INVESTMENT;
    }
    // End Agentic Workflow Section - 7. calculate profit

    public static double calculateCompoundedReturnRate(int fromPos, int toPos, TokenData tokenData) {
        double compoundedReturnRate = 0.0;
        boolean holdFlag = false;
        double currentHoldings = INITIAL_INVESTMENT;
        double holdTokenAmount = 0.0;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = ORIGINAL_DATA.getClose();

        for (int i = fromPos + 1; i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) {
                continue;
            }

            if (orderSignal[i] == 1 && !holdFlag) {
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;

                holdTokenAmount = currentHoldings / closePrice[i];
                holdFlag = true;

            } else if (orderSignal[i] == -1 && holdFlag) {
                double holdingsAfterGain = holdTokenAmount * closePrice[i];

                double fee = FEE_RATE * holdingsAfterGain;
                double finalHoldings = holdingsAfterGain - fee;

                double logReturn = Math.log(finalHoldings / currentHoldings);
                compoundedReturnRate += logReturn * 100.0;

                currentHoldings = finalHoldings;
                holdFlag = false;
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

        int lossBitLength = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos();
        double lossMid = Math.pow(2, lossBitLength) / 2.0;
        int rawLoss = (int) Math.round((avgLossMin * INDICATOR_DIVIDER) + lossMid);
        intToBits(seed, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos(), rawLoss);

        int gainBitLength = bitPositions.get("AVG_GAIN_MIN").getEndPos() - bitPositions.get("AVG_GAIN_MIN").getStartPos();
        double gainMid = Math.pow(2, gainBitLength) / 2.0;
        int rawGain = (int) Math.round((avgGainMin * INDICATOR_DIVIDER) + gainMid);
        intToBits(seed, bitPositions.get("AVG_GAIN_MIN").getStartPos(), bitPositions.get("AVG_GAIN_MIN").getEndPos(), rawGain);

        intToBits(seed, bitPositions.get("PERIOD1").getStartPos(), bitPositions.get("PERIOD1").getEndPos(), period1);
        intToBits(seed, bitPositions.get("PERIOD2").getStartPos(), bitPositions.get("PERIOD2").getEndPos(), period2);
        intToBits(seed, bitPositions.get("PERIOD3").getStartPos(), bitPositions.get("PERIOD3").getEndPos(), period3);

        System.out.println("SEED DEBUG:");
        System.out.printf("Input avgLossMin = %.6f, raw = %d, decoded = %.6f%n",
                avgLossMin, rawLoss, (rawLoss - lossMid) / INDICATOR_DIVIDER);
        System.out.printf("Input avgGainMin = %.6f, raw = %d, decoded = %.6f%n",
                avgGainMin, rawGain, (rawGain - gainMid) / INDICATOR_DIVIDER);
        System.out.printf("Periods: %d, %d, %d%n", period1, period2, period3);

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
            int rawValue = bitsToInt(candidate, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos());
            int bitLength = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos();
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

            if (period1 < 2 || period2 < 2 || period3 < 2 || period2 > period3 || (period1 + period3) > ORIGINAL_DATA.getClose().length) {
                return 0;
            }

            TokenData tokenData = generateCustomSignals(avgLossMin, avgGainMin, period1, period2, period3);
            int fromPos = (period1 + period3);
            int toPos = ORIGINAL_DATA.getClose().length - 1;

            double profit = calculateProfit(fromPos - 1, toPos, tokenData);
            double hodlProfit = executeStandardModel(fromPos - 1, toPos);

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

            int rawValue = bitsToInt(bestCandidate, bitPositions.get("AVG_LOSS_MIN").getStartPos(), bitPositions.get("AVG_LOSS_MIN").getEndPos());
            int bitLength = bitPositions.get("AVG_LOSS_MIN").getEndPos() - bitPositions.get("AVG_LOSS_MIN").getStartPos();
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
