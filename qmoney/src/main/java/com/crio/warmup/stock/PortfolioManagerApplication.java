
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

    private static String getToken(){
        return "563215e0b5346ebd0652d0754f62e58e34315a8e";
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


  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.







  // TODO: CRIO_TASK_MODULE_REST_API
  //  Find out the closing price of each stock on the end_date and return the list
  //  of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  //    and deserialize the results in List<Candle>



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
     String resultOfResolveFilePathArgs0 = "";
     String toStringOfObjectMapper = "";
     String functionNameFromTestFileInStackTrace = "";
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
        RestTemplate restTemplate = new RestTemplate();
        for(PortfolioTrade trade:portfolioTrades){
            if(endDate.isBefore(trade.getPurchaseDate())){
                throw new RuntimeException("EndDate is less then "+trade.getSymbol()+" purchase date");
            }
            List<TiingoCandle> tiingoCandles = Arrays.asList(
                    restTemplate.getForObject(prepareUrl(trade,endDate,getToken())
                    , TiingoCandle[].class));
            if(tiingoCandles.size()>0)
                totalReturnsDtos.add(new TotalReturnsDto(trade.getSymbol(),
                        tiingoCandles.get(tiingoCandles.size()-1).getClose()));
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

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainReadFile(args));
    printJsonObject(mainReadQuotes(args));

  }
}

