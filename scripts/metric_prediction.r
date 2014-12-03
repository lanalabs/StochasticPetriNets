library(forecast)
##
# Performs the actual fitting of the arima models
##
doArimaFit <- function(y1, k=7, Xmat, split=T){
  NonNAindex <- which(!is.na(y1))
  firstNonNA <- min(NonNAindex)
  ind <- (firstNonNA:length(y1))
  y1 <- zoo:::na.locf(y1)
  y <- log(y1)
  
  if (split){
    y <- forecast:::msts(y, seasonal.periods=c(k)) # multiseasonal timeseries (daily) (24, 24*k)
  } else {
    y <- forecast:::msts(y, seasonal.periods=c(24, 24 * k))
  }
  if (nrow(Xmat) > 1){
    fit <- forecast:::auto.arima(y, max.p = 20, max.q = 20, xreg = Xmat[ind,], approximation=T,trace=T)  
  } else {
    fit <- forecast:::auto.arima(y, max.p = 20, max.q = 20, xreg = data.matrix(Xmat), approximation=T,trace=T)
  }
  fit
}

##
# Uses fit objects and predicts for each of them a new value
# 
##
doForecast <- function(fit, new, predictDuration = T, weekday.eff=F){
  thisNew <- new
  thisNew$timestamp <- as.POSIXct(thisNew$timestamp / 1000, origin = "1970-01-01")
  
  if(predictDuration){
    keeps <- grep("duration|decision", colnames(thisNew), invert = T)
    thisNew <- thisNew[,keeps,drop=F]
  } else {
    decisions <- colnames(thisNew)[grep("decision", colnames(thisNew))]
    keeps <- grep("duration|decision", colnames(thisNew), invert = T)
    thisNew <- thisNew[,keeps,drop=F]
  }
  Xmatnew <- NULL
  wdtnew <- factor(weekdays(thisNew$timestamp))
  if (weekday.eff == TRUE){
    mydays <- seq(as.Date("2010-01-01"), as.Date("2010-02-01"), by="1 day")
    wdmnew <- sapply(1:nrow(thisNew),
                     function(x)(levels(factor(weekdays(mydays))) %in% weekdays(new[x,]$timestamp))[-1] + 0)
    wdmnew <- data.matrix(t(wdmnew))
    colnames(wdmnew) <- levels(factor(weekdays(mydays)))[-1]
  }
  thisNew$weekday <- as.POSIXlt(thisNew$timestamp)$wday
  thisNew$hours <- as.POSIXlt(thisNew$timestamp)$hour
  
  thisNew <- as.data.frame(thisNew[, -1])
  if (ncol(thisNew) > 1) Xmatnew <- data.matrix(thisNew)
  if (weekday.eff == TRUE){
    Xmatnew <-cbind(Xmatnew, data.frame(wdmnew))
  }
 
  
  p <- c()
  if (predictDuration){
    fc <- forecast:::forecast(fit, h = 1,  xreg = Xmatnew) # TODO: h should be the difference of last obs. to now!
    if (nrow(Xmatnew) == 1){
      p <- c(exp(fc$mean), apply(fc$lower, 2, exp) ,apply(fc$upper, 2, exp))
      names(p) <-c("Point Forecast","Lo 80","Lo 95","Hi 80", "Hi 95")
    } else {
      p <- cbind(exp(fc$mean), apply(fc$lower, 2, exp) ,apply(fc$upper, 2, exp))
      colnames(p) <-c("Point Forecast","Lo 80","Lo 95","Hi 80", "Hi 95")
    }
    p
  } else { # for decisions fit is a vector of fits
    for (i in seq(from=1, to=length(fit))){
      fc <- forecast:::forecast(fit[[i]], h = 1,  xreg = Xmatnew) # TODO: h should be the difference of last obs. to now!
      if (nrow(Xmatnew) == 1){
        pot <- c(exp(fc$mean), apply(fc$lower, 2, exp) ,apply(fc$upper, 2, exp))
        names(pot) <-c("Point Forecast","Lo 80","Lo 95","Hi 80", "Hi 95")
      } else {
        pot <- cbind(exp(fc$mean), apply(fc$lower, 2, exp) ,apply(fc$upper, 2, exp))
        colnames(pot) <-c("Point Forecast","Lo 80","Lo 95","Hi 80", "Hi 95")
      }
      if (!is.null(p)) {
        p <- rbind(p, pot)
        rownames(p)[i] = decisions[i]
      } else {
        p <- pot
      }
    }
    rownames(p)[1] = decisions[1] 
    p
  }
  
}
  
  
  

##
# performs a fit of the auto arima model (selecting the best model according to the AIC)
#
# predictDuration=T means that the duration is to be predicted
#
# returns either a fit-object for predicting the next duration (predictDuration=T)
# or a vector of fit-objects for predicting the next decision.
##
metricFit <- function(df, weekday.eff = F, holiday.eff = TRUE, split = FALSE, predictDuration = T){
  df$timestamp <- as.Date(df$timestamp)
  
  k <-  length(levels(factor(weekdays(df$timestamp)))) #  period week
  if (weekday.eff == TRUE){
    wdt <- factor(weekdays(df$timestamp))
    #wdm <- cbind(model.matrix(~ factor( weekdays(df$timestamp)) - 1 )[, -1])
    wdm <- model.matrix(~ factor(weekdays(df$timestamp)))[,-1]
    
    colnames(wdm) <- levels(factor(weekdays(df$timestamp)))[-1]
  }
  df$weekday <- as.POSIXlt(df$timestamp)$wday
  if (predictDuration){
    y1 <- as.numeric(df[,grep("duration", colnames(df))])
    keeps <- grep("decision|duration",colnames(df), invert = T)
    df <- df[,keeps,drop=F]
  } else {
    decisions <- df[,grep("decision", colnames(df)), drop=F]
    y1 <- data.matrix(decisions)
    keeps <- grep("duration|decision",colnames(df), invert = T)
    df <- df[,keeps,drop=F]
    #y1 <- as.numeric(df[,grep("decision", colnames(df))])
  }
  Xmat <- NULL
  if (ncol(df) > 1) Xmat <- data.matrix(df[, -1, drop=F])
  
  if (weekday.eff == TRUE){
    Xmat <- cbind(Xmat, wdm)
  }
  y1[y1 == 0] <- NA # for 0 values -> discard or 0.0001?
  if (predictDuration){
    fit <- doArimaFit(y1=y1, k=k, Xmat = Xmat, split=split)
  } else {
    # perform the fit for all decisions:
    fit <- list()
    for (i in seq(from = 1, to = ncol(decisions))){
      resultFit <- doArimaFit(y1=y1[,i,drop=F], k=k, Xmat = Xmat, split=split)
      fit[i] <- list(resultFit)
      #names(fit)[i] = colnames(decisions)[i]
    }
  }
  fit
} 

runPredCategory <- function(){
  df <- runCategoricalPrediction(fileName='/tmp/training2336572051550998672.csv', timeStamp=1415711202046)
  new <- read.csv('/tmp/input7695501530806246324.csv', sep = ";")
  fit <- metricFit(df=df, predictDuration=F)
  prediction <- doForecast(fit = fit, new=new, predictDuration=F)
}

runPredTime <- function(){
  df <- runCategoricalPrediction(fileName='/tmp/training7213179702572454521.csv', timeStamp=1415726489742)
  new <- read.csv('/tmp/input6210829645568432654.csv', sep = ";")
  fit <- metricFit(df=df, predictDuration=T)
  prediction <- doForecast(fit=fit, new=new, predictDuration=T)
}

runNewDecision <-function(){
  df <- runCategoricalPrediction(fileName='/tmp/training1449070856211178158.csv', timeStamp=1415811523189)
  fit <- metricFit(df=df, predictDuration=F)
  new <- read.csv('/tmp/input5006765840523468622.csv', sep = ";")
  prediction <- doForecast(fit=fit, new=new, predictDuration=F)
}