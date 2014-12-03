library(forecast)

getForecast <- function(fit, h=10){
  fc <- forecast:::forecast(fit, h = h)
  #p <- c(exp(fc$mean), apply(fc$lower, 2, exp) ,apply(fc$upper, 2, exp))
  p <- c(fc$mean, fc$lower, fc$upper)
  p
}

## x is the univariate time series
## k is the period (if we have a 7 day week, it is 7 - otherwise, if we already removed weekends, it can be also 5)
## split is an indicator telling us whether we only have the data for the season, we try to forecast 
## (e.g., only data from 9 o'clock each day when trying to forecast something that would happen at 9 o'clock next day.)
getFit <- function(y, k=7, split=F){
  # split means that data is split into only the hours 
  if (split){
    y <- forecast:::msts(y, seasonal.periods=c(k)) # multiseasonal timeseries (daily) (24, 24*k)
  } else {
    y <- forecast:::msts(y, seasonal.periods=c(24, 24 * k))
  }
  # handle NAs!
  # y <- na.interp(y) 
  y <- tsclean(y)
  #print(y)

  fit <- forecast:::auto.arima(y, max.p = 20, max.q = 20, approximation=T, trace=T)
  fit
}