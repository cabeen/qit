################################################################################
# Custom Functions                                                             #
# Author: Ryan Cabeen                                                          #
################################################################################
# This contains a collection of custom functions that are likely to be used by #
# multiple programs.                                                           #
################################################################################

scd <- function(x, y) 
{
  # Compute the signed Cohen's d effect size between two sets of measurements
  # 
  # Args:
  #   x: One of two vectors
  #   y: The other vector. x and y must have the same length, greater than one,
  #      with no missing values.
  # 
  # Returns:
  #   The signed Cohen's d

  if (TRUE %in% is.na(x) || TRUE %in% is.na(y)) 
  {
    stop(" Arguments x and y must not have missing values.")
  }

  lx <- length(x)- 1
  ly <- length(y)- 1
  md  <- mean(x) - mean(y)             ## mean difference (numerator)
  csd <- lx * var(x) + ly * var(y)
  csd <- csd/(lx + ly)
  csd <- sqrt(csd)                     ## common sd computation

  cd  <- md/csd                        ## cohen's d

  return(cd)
}

cd <- function(x, y) 
{
  # Compute the signed Cohen's d effect size between two sets of measurements
  # 
  # Args:
  #   x: One of two vectors
  #   y: The other vector. x and y must have the same length, greater than one,
  #      with no missing values.
  # 
  # Returns:
  #   The signed Cohen's d

  if (TRUE %in% is.na(x) || TRUE %in% is.na(y)) 
  {
    stop(" Arguments x and y must not have missing values.")
  }

  lx <- length(x)- 1
  ly <- length(y)- 1
  md  <- abs(mean(x) - mean(y))         ## mean difference (numerator)
  csd <- lx * var(x) + ly * var(y)
  csd <- csd/(lx + ly)
  csd <- sqrt(csd)                      ## common sd computation

  scd  <- md/csd                        ## cohen's d

  return(scd)
}

cv <- function(x, y) 
{
  # Compute the coefficient of variation between two sets of measurements
  # 
  # Args:
  #   x: One of two vectors
  #   y: The other vector. x and y must have the same length, greater than one,
  #      with no missing values.
  # 
  # Returns:
  #   The coefficient of variation 

  n <- length(x)

  if (TRUE %in% is.na(x) || TRUE %in% is.na(y)) 
  {
    stop(" Arguments x and y must not have missing values.")
  }

  md  <- (mean(x) - mean(y))
  sd  <- (mean(x) + mean(y))

  cv  <- 2.0 * md/ sd
  
  return(cv)
}

se = function(x)
{
  # Compute the standard error of the mean of a set of measurements
  # 
  # Args:
  #   x: the measurements with no missing values 
  # 
  # Returns:
  #   The coefficient of variation 

  if (TRUE %in% is.na(x))
  {
    stop(" Arguments x and y must not have missing values.")
  }

  se = sd(x) / sqrt(length(x))

  return(se)
}

ci = function(x, level=0.95)
{
  # Compute the confidence interval of the mean of a set of measurements
  # 
  # Args:
  #   x: the measurements with no missing values 
  # 
  # Returns:
  #   The coefficient of variation 

  if (TRUE %in% is.na(x))
  {
    stop(" Arguments x and y must not have missing values.")
  }

  ci = qt(level, length(x) - 1) * se(x)
  
  return(ci)
}

# Summarizes data.
# Gives count, mean, standard deviation, standard error of the mean, and confidence interval (default 95%).
#  data: a data frame.
#  measurevar: the name of a column that contains the variable to be summariezed
#  groupvars: a vector containing names of columns that contain grouping variables
#  na.rm: a boolean that indicates whether to ignore NA's
#  conf.interval: the percent range of the confidence interval (default is 95%)
# 
#  Source: http://www.cookbook-r.com/Graphs/Plotting_means_and_error_bars_(ggplot2)
#  Downloaded: October 24, 2014
#
summarySE <- function(data=NULL, measurevar, groupvars=NULL, na.rm=FALSE,
                      conf.interval=.95, .drop=TRUE) {
    require(plyr)

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

# Multiple plot function
#
# ggplot objects can be passed in ..., or to plotlist (as a list of ggplot objects)
# - cols:   Number of columns in layout
# - layout: A matrix specifying the layout. If present, 'cols' is ignored.
#
# If the layout is something like matrix(c(1,2,3,3), nrow=2, byrow=TRUE),
# then plot 1 will go in the upper left, 2 will go in the upper right, and
# 3 will go all the way across the bottom.
#
#  Source: http://www.cookbook-r.com/Graphs/Multiple_graphs_on_one_page_(ggplot2)
#  Downloaded: October 24, 2014
#
multiplot <- function(..., plotlist=NULL, file, cols=1, layout=NULL) {
  require(grid)

  # Make a list from the ... arguments and plotlist
  plots <- c(list(...), plotlist)

  numPlots = length(plots)

  # If layout is NULL, then use 'cols' to determine layout
  if (is.null(layout)) {
    # Make the panel
    # ncol: Number of columns of plots
    # nrow: Number of rows needed, calculated from # of cols
    layout <- matrix(seq(1, cols * ceiling(numPlots/cols)),
                    ncol = cols, nrow = ceiling(numPlots/cols))
  }

 if (numPlots==1) {
    print(plots[[1]])

  } else {
    # Set up the page
    grid.newpage()
    pushViewport(viewport(layout = grid.layout(nrow(layout), ncol(layout))))

    # Make each plot, in the correct location
    for (i in 1:numPlots) {
      # Get the i,j matrix positions of the regions that contain this subplot
      matchidx <- as.data.frame(which(layout == i, arr.ind = TRUE))

      print(plots[[i]], vp = viewport(layout.pos.row = matchidx$row,
                                      layout.pos.col = matchidx$col))
    }
  }
}
