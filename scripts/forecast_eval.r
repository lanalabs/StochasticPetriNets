library(readr)
library(plyr)
library(reshape)

setwd("/home/andreas/dev/Workspace/StochasticPetriNets")

prediction_results <- read_csv("./prediction_results.csv")

prediction_results$dur_days <- prediction_results$real_duration / (1000*60*60*24)
prediction_results$pred_dur_days <- prediction_results$prediction / (1000*60*60*24)

prediction_results$error <- (prediction_results$prediction - prediction_results$real_duration) / (1000 * 60 * 60 * 24) # in days 

prediction_results$squared_error = prediction_results$error * prediction_results$error

prediction_results_agg <- ddply(prediction_results, "method",  summarise, mean_error = mean(error), mean_squared_error = mean(squared_error))
