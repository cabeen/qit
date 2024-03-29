#! /usr/bin/env R CMD BATCH

library(ggplot2)

summarySE <- function(data=NULL, measurevar, groupvars=NULL, na.rm=FALSE,
                      conf.interval=.95, .drop=TRUE) {
    library(plyr)

    # New version of length which can handle NA's: if na.rm==T, don't count them
    length2 <- function (x, na.rm=FALSE) {
        if (na.rm) sum(!is.na(x))
        else       length(x)
    }

    # This does the summary. For each group's data frame, return a vector with
    # N, mean, and sd
    datac <- ddply(data, groupvars, .drop=.drop,
      .fun = function(xx, col) {
        c(N    = length2(xx[[col]], na.rm=na.rm),
          mean = mean   (xx[[col]], na.rm=na.rm),
          sd   = sd     (xx[[col]], na.rm=na.rm)
        )
      },
      measurevar
    )

    # Rename the "mean" column    
    datac <- rename(datac, c("mean" = measurevar))

    datac$se <- datac$sd / sqrt(datac$N)  # Calculate standard error of the mean

    # Confidence interval multiplier for standard error
    # Calculate t-statistic for confidence interval: 
    # e.g., if conf.interval is .95, use .975 (above/below), and use df=N-1
    ciMult <- qt(conf.interval/2 + .5, datac$N-1)
    datac$ci <- datac$se * ciMult

    return(datac)
}

# Run the simulation and load the results

bn <- "EvaluateBiExpDecayFit"
fn <- "data.csv"
system(sprintf("qit %s --output %s", bn, fn))
df <- read.csv(fn)

# Plot the data

for (param in c("totalerr", "sigerr", "alphaerr", "fracerr", "betaerr", "gammaerr", "alphadel", "fracdel", "betadel", "gammadel", "alphaest", "fracest", "betaest", "gammaest"))
{
  df$value <- df[,param]
  dfs <- summarySE(df, measurevar="value", groupvars=c("method", "noise"))
  myplot <- ggplot(data=dfs, aes(x=noise, y=value, group=method, fill=method)) 
  myplot <- myplot + geom_bar(stat="identity", position=position_dodge()) 
  myplot <- myplot + geom_errorbar(aes(ymin=value-se, ymax=value+se), width=.2, position=position_dodge(.9))
  myplot <- myplot + ggtitle(param)
  ggsave(sprintf("plot.%s.pdf", param))
}

