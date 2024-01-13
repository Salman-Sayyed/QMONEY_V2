package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.crio.warmup.stock.portfolio.PortfolioManagerImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;



public class PortfolioManagerApplication {


    public static String getToken(){
        String filePath = "config.json";
        ObjectMapper oM = getObjectMapper();
        File configFile;
        Secrets secret;
        try {
            configFile = resolveFileFromResources(filePath);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        try {
            secret = oM.readValue(configFile,Secrets.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return secret.TIINGO_API_TOKEN;
    }

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
      String fileName = args[0];
      File inputFile = resolveFileFromResources(fileName);
      ObjectMapper objectMapper = getObjectMapper();
      List<PortfolioTrade> portfolioTradesList = objectMapper.readValue(
              inputFile
              , new TypeReference<List<PortfolioTrade>>() {});
     return portfolioTradesList
             .stream()
             .map(PortfolioTrade::getSymbol)
             .collect(Collectors.toList());
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }


  public static List<String> debugOutputs() {

     String valueOfArgument0 = "trades.json";
     String resultOfResolveFilePathArgs0 = "trades.json";
     String toStringOfObjectMapper = "ObjectMapper";
     String functionNameFromTestFileInStackTrace = "mainReadFile";
     String lineNumberFromTestFileInStackTrace = "";


    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});
  }


  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.
  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
        String fileName = args[0];
        LocalDate endDate = LocalDate.parse(args[1]);
        List<PortfolioTrade> portfolioTrades = readTradesFromJson(fileName);
        List<TotalReturnsDto> totalReturnsDtos = new ArrayList<>();
        for(PortfolioTrade trade:portfolioTrades){
            if(endDate.isBefore(trade.getPurchaseDate())){
                throw new RuntimeException("EndDate is less then "+trade.getSymbol()+" purchase date");
            }
            List<Candle> candles = fetchCandles(trade,endDate,getToken());
            if(candles.size()>0)
                totalReturnsDtos.add(new TotalReturnsDto(trade.getSymbol(),
                        getClosingPriceOnEndDate(candles)));
        }
        Collections.sort(totalReturnsDtos);
        return totalReturnsDtos.stream().map(TotalReturnsDto::getSymbol).collect(Collectors.toList());
  }

  // TODO:
  //  After refactor, make sure that the tests pass by using these two commands
  //  ./gradlew test --tests PortfolioManagerApplicationTest.readTradesFromJson
  //  ./gradlew test --tests PortfolioManagerApplicationTest.mainReadFile
  public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
      File inputFile = resolveFileFromResources(filename);
      ObjectMapper objectMapper = getObjectMapper();
     return objectMapper.readValue(inputFile
             , new TypeReference<List<PortfolioTrade>>() {});
  }


  // TODO:
  //  Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("https://api.tiingo.com/tiingo/daily/");
        stringBuilder.append(trade.getSymbol());
        stringBuilder.append("/prices?startDate=");
        stringBuilder.append(trade.getPurchaseDate().toString());
        stringBuilder.append("&endDate=");
        stringBuilder.append(endDate.toString());
        stringBuilder.append("&token="+token);
     return stringBuilder.toString();
  }
  // TODO:
  //  Ensure all tests are passing using below command
  //  ./gradlew test --tests ModuleThreeRefactorTest
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
     return candles.get(0).getOpen();
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
     return candles.get(candles.size()-1).getClose();
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
      RestTemplate restTemplate = new RestTemplate();
      return Arrays.asList(restTemplate.getForObject(
                prepareUrl(trade,endDate,getToken()),
                TiingoCandle[].class));
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
        String fileName = args[0];
        LocalDate endDate = LocalDate.parse(args[1]);
        List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
        List<PortfolioTrade> trades = readTradesFromJson(fileName);
        for(PortfolioTrade trade:trades){
            List<Candle> candles = fetchCandles(trade,endDate,getToken());
            annualizedReturns.add(calculateAnnualizedReturns(endDate,
                    trade,getOpeningPriceOnStartDate(candles),
                    getClosingPriceOnEndDate(candles)));
        }
        Collections.sort(annualizedReturns,Collections.reverseOrder());
     return annualizedReturns;
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Return the populated list of AnnualizedReturn for all stocks.
  //  Annualized returns should be calculated in two steps:
  //   1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  //      1.1 Store the same as totalReturns
  //   2. Calculate extrapolated annualized returns by scaling the same in years span.
  //      The formula is:
  //      annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  //      2.1 Store the same as annualized_returns
  //  Test the same using below specified command. The build should be successful.
  //     ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

    // returns HoldingPeriodReturn (HPR)
    public static Double getHoldingPeriodReturn(Double buyPrice,Double sellPrice){
        Double result = (sellPrice-buyPrice)/buyPrice;
        return result;
    }

    public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
            //HPR is HoldingPeriodReturn
            Double HPR = getHoldingPeriodReturn(buyPrice, sellPrice);
            Double time = trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS)/365.24;
            Double result = Math.pow((1+HPR),(1/time))-1;
            return new AnnualizedReturn(trade.getSymbol(), result, sellPrice-buyPrice);

        }

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       String file = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       String contents = readFileAsString(file);
       ObjectMapper objectMapper = getObjectMapper();
       RestTemplate restTemplate = getRestTemplate();
       PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
       PortfolioTrade[] trades = objectMapper.readValue(contents,PortfolioTrade[].class);
       return portfolioManager.calculateAnnualizedReturn(Arrays.asList(trades), endDate);
  }

    private static String readFileAsString(String file) throws Exception {
        File jsonFile = resolveFileFromResources(file);
        Scanner scanner = new Scanner(jsonFile);
        scanner.useDelimiter("\\Z");
        return scanner.next();
    }

    private static RestTemplate getRestTemplate(){
        return new RestTemplate();
  }


  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());
//    printJsonObject(mainReadFile(args));
//    printJsonObject(mainReadQuotes(args));
//    printJsonObject(mainCalculateSingleReturn(args));
    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}

