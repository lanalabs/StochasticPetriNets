# df contains the data to use. make sure that categorical variables are saved as factor i.e., df$cat <- factor(df$cat)
# weekend.days = c("Monday", "Tuesday")

aggregateHourlyData <- function(df, elim.weekend = FALSE, weekend.days = NA,split = T){
  vars <- as.data.frame(df[, !(colnames(df) %in% "timestamp")])
  colnames(vars) <- colnames(df)[!(colnames(df) %in% "timestamp")]
  # if time stamp not formatted, format:
  df$timestamp <- as.POSIXct(df$timestamp / 1000, origin = "1970-01-01")
  #df$timestamp <- strptime(df$timestamp, format = "%y%m%d %H:%M:%S CET")
  hFac <- droplevels(cut(df$timestamp, breaks="hour"))
  # average both variables over days within each hour and host
  ll <- lapply(vars, function(x)
    if (class(x) == "factor"){
      agg <- (aggregate(x ~ hFac, FUN=function(z) table(z)))
      data.frame(hFac= as.character(agg$hFac),   cbind(agg$x))
      #cbind(hFac=as.character(agg[,1]), do.call("cbind", agg)[,-1])
    } else {
      aggregate(x ~ hFac, FUN = function(z) quantile(z, 0.5))
    }
  )
  l <- do.call("cbind", ll)
  if (length(grep("hFac", colnames(l))[-1])!=0)  l <- l[, -(grep("hFac", colnames(l))[-1])]
  colnames(l)[grep("hFac", colnames(l))[1]] <- "timestamp"
  colnames(l)[grep("duration", colnames(l))[1]] <- "duration"
  colnames(l)[grep("systemLoad", colnames(l))[1]] <- "systemLoad"
  
  l$timestamp <- as.POSIXct(l$timestamp)
  l$weekday <- as.POSIXlt(l$timestamp)$wday
  date1 <- df$timestamp[1] - 30*60

  # todo: aggregate more sensibly!
  
  dates <-  seq(round(date1, units = "hours"),
                round(df$timestamp[nrow(df)], units="hours"), by="hour")
  l <- as.data.frame(l)
  newdf <- as.data.frame(matrix(ncol= ncol(l), nrow = length(dates)))
  newdf[,1] <- dates
  newdf[which(dates %in% l$timestamp), - 1] <- l[, -1]
  newdf$hours <- as.factor(as.POSIXlt(newdf[,1])$hour)  # extract hour of the day
  colnames(newdf)[1:ncol(l)] <- colnames(l)
  wdt <- weekdays(newdf$timestamp)
  if (elim.weekend){
    newdf <- newdf[!(wdt %in% weekend.days), ]
  }
  if (split){
    data <- split(newdf[1:ncol(l)], newdf$hours)
    # fill in missing values
    lapply(data, zoo:::na.locf)
  } else {
   # newdf$weekday <- wdt
    newdf <- zoo:::na.locf(newdf)
    newdf
  }
}
#alldata <- aggregateHourlyData(df1) 


# factorizes only the decision column currently -> need to make this more flexible for more categorical values
runCategoricalPrediction <- function(fileName = "example_prediction_data.csv", sep=";", timeStamp = Sys.time(), split = F) {
  if (is.numeric(timeStamp)){
    timeStamp <- as.POSIXct(timeStamp / 1000, origin = "1970-01-01")
  }
  hour <- timeStamp - 30*60
  hour <-  round(hour, units = "hours")
#  str(hour)
  hour <- unclass(as.POSIXlt(hour))$hour
#  print(hour)

  df1 <- read.csv(fileName, sep = ";")
  if("decision" %in% colnames(df1)) df1$decision <- factor(df1$decision)
  allData <- aggregateHourlyData(df1, split=F)
#  print(names(allData))
  if (split) {
    allData[(names(allData) == as.character(hour))][[1]]
  } else {
    allData
  }
}

runCategoricalPredictionJRI <- function(data, timeStamp = Sys.time(), split = F) {
  if (is.numeric(timeStamp)){
    timeStamp <- as.POSIXct(timeStamp / 1000, origin = "1970-01-01")
  }
  hour <- timeStamp - 30*60
  hour <-  round(hour, units = "hours")
  #  str(hour)
  hour <- unclass(as.POSIXlt(hour))$hour
  #  print(hour)
  
  df1 <- data.frame(data)
  if("decision" %in% colnames(df1)) df1$decision <- factor(df1$decision)
  allData <- aggregateHourlyData(df1, split=F)
  #  print(names(allData))
  if (split) {
    allData[(names(allData) == as.character(hour))][[1]]
  } else {
    allData
  }
}

