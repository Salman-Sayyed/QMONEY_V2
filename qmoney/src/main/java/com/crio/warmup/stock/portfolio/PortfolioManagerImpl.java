
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

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

  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    Candle[] candles = restTemplate.getForObject(buildUri(symbol,from,to),TiingoCandle[].class);
     return Arrays.asList(candles);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       String uriTemplate = "https:api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+
               startDate.toString()+"&endDate="+endDate.toString()+"&token="+getToken();
       return uriTemplate;
  }

  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size()-1).getClose();
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) {
    List<AnnualizedReturn> returns = new ArrayList<>();
    for(PortfolioTrade trade:portfolioTrades){
      List<Candle> candles;
        try {
            candles = getStockQuote(trade.getSymbol(),trade.getPurchaseDate(),endDate);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
      returns.add(calculateAnnualizedReturns(endDate,trade,
              getOpeningPriceOnStartDate(candles), getClosingPriceOnEndDate(candles)));
    }
    Collections.sort(returns,getComparator());
    return returns;
  }

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

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
            Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }
}
