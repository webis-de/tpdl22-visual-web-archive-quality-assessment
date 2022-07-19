import numpy as np
import matplotlib.pyplot as plt
import pandas
from sklearn import linear_model
from scipy import stats
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import r2_score
from sklearn.metrics import confusion_matrix
from sklearn.metrics import mean_absolute_error
import sklearn.feature_selection as s
import math
import statistics
from itertools import chain, combinations
import seaborn as sns

# https://docs.python.org/3/library/itertools.html#itertools-recipes
def powerset(iterable):
    "powerset([1,2,3]) --> () (1,) (2,) (3,) (1,2) (1,3) (2,3) (1,2,3)"
    s = list(iterable)
    return chain.from_iterable(combinations(s, r) for r in range(len(s)+1))

def performBaselineRegressionAll(baselineX, y):
    baselineRegr = linear_model.LinearRegression()
    baselineRegr.fit(baselineX, y)
    predicted_y = baselineRegr.predict(baselineX)
    rounded_y = predicted_y.copy()
    #for i in range(0, len(predicted_y)):
    #	rounded_y[i] = round(predicted_y[i]) # int(predicted_y) can be 0 and will never be 5 r2 => -0.8
    r2 = r2_score(y, predicted_y)
    return r2

def performReconstructionRegressionAll(reconstructionX, y):
    reconstructionRegr = linear_model.LinearRegression()
    reconstructionRegr.fit(reconstructionX, y)
    predicted_y = reconstructionRegr.predict(reconstructionX)
    rounded_y = predicted_y.copy()
    #for i in range(0, len(predicted_y)):
    #	rounded_y[i] = round(predicted_y[i]) # int(predicted_y) can be 0 and will never be 5 r2 => -0.8
    r2 = r2_score(y, predicted_y)
    return r2

def scaleX(df, shouldScale):
    # scale using minmax scaler
    # reason: distribution of X is unknown, min max does not change it
    # columns will be scaled to a range of min max
    if(shouldScale):
        minmaxScaler = MinMaxScaler()
        X = df[['pixels added through insertion/deletion','total number of changed pixels','pixels total in original','changed pixels as percent of pixels in original','number of pixels in bigger image','changed pixels as percent of pixels in bigger image','insertion-deletion-error','total-number-of-changed-pixels','pixels-total-in-original','changed-pixels-as-percent-of-pixels-in-original','number-of-pixels-in-bigger-image','changed-pixels-as-percent-of-pixels-in-bigger-image','count','shifts total','shiftsToTop','shiftsToTopRight','shiftsToRight','shiftsToBottomRight','shiftsToBottom','shiftsToBottomLeft','shiftsToLeft','shiftsToTopLeft','small shifts total','small shiftsToTop','small shiftsToTopRight','small shiftsToRight','small shiftsToBottomRight','small shiftsToBottom','small shiftsToBottomLeft','small shiftsToLeft','small shiftsToTopLeft','big shifts total','big shiftsToTop','big shiftsToTopRight','big shiftsToRight','big shiftsToBottomRight','big shiftsToBottom','big shiftsToBottomLeft','big shiftsToLeft','big shiftsToTopLeft']]
        scaledX = minmaxScaler.fit_transform(X)
        Xdf = pandas.DataFrame(data=scaledX, columns=['pixels added through insertion/deletion','total number of changed pixels','pixels total in original','changed pixels as percent of pixels in original','number of pixels in bigger image','changed pixels as percent of pixels in bigger image','insertion-deletion-error','total-number-of-changed-pixels','pixels-total-in-original','changed-pixels-as-percent-of-pixels-in-original','number-of-pixels-in-bigger-image','changed-pixels-as-percent-of-pixels-in-bigger-image','count','shifts total','shiftsToTop','shiftsToTopRight','shiftsToRight','shiftsToBottomRight','shiftsToBottom','shiftsToBottomLeft','shiftsToLeft','shiftsToTopLeft','small shifts total','small shiftsToTop','small shiftsToTopRight','small shiftsToRight','small shiftsToBottomRight','small shiftsToBottom','small shiftsToBottomLeft','small shiftsToLeft','small shiftsToTopLeft','big shifts total','big shiftsToTop','big shiftsToTopRight','big shiftsToRight','big shiftsToBottomRight','big shiftsToBottom','big shiftsToBottomLeft','big shiftsToLeft','big shiftsToTopLeft'])
        return(Xdf)
    else:
        return(df)

def useRoundingMethod(method, predicted_test_y):
    rounded_test_y = predicted_test_y.copy()
    if(method == "round"):
        for i in range(0, len(predicted_test_y)):
            rounded_test_y[i] = round(predicted_test_y[i])
    elif(method == "ceil"):
        for i in range(0, len(predicted_test_y)):
            rounded_test_y[i] = math.ceil(predicted_test_y[i])
    elif(method == "floor"):
        for i in range(0, len(predicted_test_y)):
            rounded_test_y[i] = math.floor(predicted_test_y[i])
    elif(method == "int"):
        for i in range(0, len(predicted_test_y)):
            rounded_test_y[i] = int(predicted_test_y[i])
    elif(method == "int + 1"):
        for i in range(0, len(predicted_test_y)):
            rounded_test_y[i] = int(predicted_test_y[i]) + 1
    elif(method == "none"):
        return predicted_test_y
    else:
        print("EXCEPTION: DID NOT ROUND!")
    return(rounded_test_y)

def performBaselineRegression(rounding_method, train_baselineX, train_y, test_baselineX, test_y):
    baselineRegr = linear_model.LinearRegression()
    baselineRegr.fit(train_baselineX, train_y)
    predicted_test_y = baselineRegr.predict(test_baselineX)
    rounded_test_y = useRoundingMethod(rounding_method, predicted_test_y)
    return(rounded_test_y)

def performReconstructionRegression(rounding_method, train_reconstructionX, train_y, test_reconstructionX, test_y):
    reconstructionRegr = linear_model.LinearRegression()
    reconstructionRegr.fit(train_reconstructionX, train_y)
    predicted_test_y = reconstructionRegr.predict(test_reconstructionX)
    rounded_test_y = useRoundingMethod(rounding_method, predicted_test_y)
    return(rounded_test_y)

def rand_jitter(arr):
    stdev = .01 * (max(arr) - min(arr))
    return arr + np.random.randn(len(arr)) * stdev

def plot_y(predicted_y, true_y, i, name):
    plt.axes(xlabel="predicted y", ylabel="true y")
    #plt.title("Fold ID: " + str(i) + ", " + name)
    plt.title(name)
    plt.xticks([-15,-10,-5,0,1,2,3,4,5,6,7,8,9,10])
    plt.yticks([0,1,2,3,4,5])
    #plt.scatter(predicted_y, true_y, alpha=0.002, color="blue")
    plt.scatter(rand_jitter(predicted_y), rand_jitter(true_y), alpha=0.2, color="blue")
    plt.show()
    #plt.savefig(str(i) + '_' + name + '.pdf')


def getTrainTestDataFromFolds(stratified, i, train, multipleSizeClasses):
    fileName = ""
    if(stratified):
        fileName = "stratifiedTrain"+str(i)+".csv" if train else "stratifiedTest"+str(i)+".csv"
    else:
        fileName = "train"+str(i)+".csv" if train else "test"+str(i)+".csv"
    dataFile = "path/to/train/test/folds" + fileName

    df = pandas.read_csv(dataFile)

    sizeClasses=[[] for i in range(3)]
    if(multipleSizeClasses and not train):
        for row in df.itertuples():
            if(True): #if(row[2] != 1 and row[2] != 2):
                # row[4] == total number of pixels in original
                if(row[4] <= 2300000):
                    #small
                    sizeClasses[0].append(row[1:])
                elif(row[4] > 2300000):
                    if(row[4] <= 4800000):
                        #medium
                        sizeClasses[1].append(row[1:])
                    else:
                        #large
                        sizeClasses[2].append(row[1:])
        smallDf= pandas.DataFrame(data=sizeClasses[0], columns=df.columns)
        mediumDf= pandas.DataFrame(data=sizeClasses[1], columns=df.columns)
        largeDf= pandas.DataFrame(data=sizeClasses[2], columns=df.columns)
        smallMediumDf = smallDf.append(mediumDf, ignore_index=True)
        smallLargeDf = smallDf.append(largeDf, ignore_index=True)
        mediumLargeDf = mediumDf.append(largeDf, ignore_index=True)
        listOfSizeClasses=[smallDf, mediumDf, largeDf, smallMediumDf, smallLargeDf, mediumLargeDf]
        return(listOfSizeClasses)

    else:
        return(df)

def removeHits(true_y, predicted_y, reduced_true_y, reduced_predicted_y):
    for i in range(0,len(true_y)):
        if(true_y[i] != predicted_y[i]):
            reduced_true_y.append(true_y[i])
            reduced_predicted_y.append(predicted_y[i])
    return


def computeConfusionMatrix(true_y, predicted_y):
    predictedForTrue1=[0,0,0,0,0,0,0,0,0,0,0,0]
    predictedForTrue2=[0,0,0,0,0,0,0,0,0,0,0,0]
    predictedForTrue3=[0,0,0,0,0,0,0,0,0,0,0,0]
    predictedForTrue4=[0,0,0,0,0,0,0,0,0,0,0,0]
    predictedForTrue5=[0,0,0,0,0,0,0,0,0,0,0,0]
    for i in range(0, len(true_y)):
        if(true_y[i] == 1):
            predictedForTrue1[int(predicted_y[i])] +=1
        elif(true_y[i] == 2):
            predictedForTrue2[int(predicted_y[i])] +=1
        elif(true_y[i] == 3):
            predictedForTrue3[int(predicted_y[i])] +=1
        elif(true_y[i] == 4):
            predictedForTrue4[int(predicted_y[i])] +=1
        elif(true_y[i] == 5):
            predictedForTrue5[int(predicted_y[i])] +=1
        else:
            print("true y out of range with true_y = " + str(true_y[i]))
    dataMatrix = np.matrix([predictedForTrue1, predictedForTrue2, predictedForTrue3, predictedForTrue4, predictedForTrue5])

    return dataMatrix

def computeAccuracy(true_y, predicted_y):
    predictions_true = 0
    predictions_total = len(true_y) # true_y and predicted_y are of same length
    print(true_y)
    for i in range(0, predictions_total):
        if(float(true_y[i]) == predicted_y[i]):
            predictions_true += 1
    accuracy = predictions_true / predictions_total
    return(accuracy)

def getSubsetOfFeatures(listOfAllFeatures, numberOfFeatures):
    features = listOfAllFeatures.copy()
    featuresPowerset = list(powerset(listOfAllFeatures))
    selectedFeatures = list(featuresPowerset[7])
    if(numberOfFeatures == 5):
        return listOfAllFeatures

    if(numberOfFeatures == -1):
        selectedFeatures = list(featuresPowerset)
    return(selectedFeatures) # TODO: maybe just hand over how many features should be used, and just return the subsets of the powerset with len = numberOfFeatures

def printResult(result, doReconstruction, r2s_Baseline, r2s_Reconstruction, accuracies_Baseline, accuracies_Reconstruction):
    print("Used stratified train/test set: " + result[0])
    print("Used scaled train/test set: " + result[1])
    print("Round function used on predicted y: " + result[2])
    print("Used Features on Reconstruction: " + result[3])
    print("-------------------------R2-------------------------------")
    print("r2 baseline per fold: " + str(r2s_Baseline))
    print("r2 baseline mean: " + result[4])

    #result = [str(stratify), str(scale), str(rounding_method), str(reconstructionFeatures), str(r2_Baseline), str(r2_Reconstruction), str(accuracy_Reconstruction)]
    if(doReconstruction):
        print("----------------------------------------------------------")
        print("r2 reconstruction per fold: " + str(r2s_Reconstruction))
        print("r2 reconstruction mean: " + result[5])

    print("-------------------------ACCURACY-------------------------")
    print("accuracy baseline per fold: " + str(accuracies_Baseline))
    print("accuracy baseline mean: " + result[6])

    if(doReconstruction):
        print("----------------------------------------------------------")
        print("accuracy reconstruction per fold: " + str(accuracies_Reconstruction))
        print("accuracy reconstruction mean: " + result[7])

def main():
    # settings
    doReconstruction = True # also do the linear regression with the set parameters on reconstuction data (not only on baseline)

    stratify = False
    scale = False
    rounding_method = "round" # possible values: "round", "ceil", "floor", "int", "int + 1", "none" (no rounding)

    plot = False # plots and saves one figure for each fold and each type (baseline or reconstruction)
    plot_all = False # plots and saves one figure for all folds

    multipleSizeClasses = False
    sizeClass = 2 # 0=small,1=medium,2=large, 3=smallmedium, 4=smalllarge, 5=mediumlarge, 6=all (helpful for grouped by qualities)

    results = []
    allBaselineFeatures = ['pixels added through insertion/deletion','total number of changed pixels','pixels total in original','changed pixels as percent of pixels in original','number of pixels in bigger image','changed pixels as percent of pixels in bigger image']
    bestBaselineFeatures_accuracy = ['pixels added through insertion/deletion','total number of changed pixels']
    bestBaselineFeatures_r2 = ['pixels added through insertion/deletion','total number of changed pixels','number of pixels in bigger image','changed pixels as percent of pixels in bigger image']
    #allBaselineFeatures = ['changed pixels as percent of pixels in original','changed pixels as percent of pixels in bigger image'] # bestBaselineFeatures_accuracy #allBaselineFeatures

    reconstructionFeatures_pure = ['insertion-deletion-error','total-number-of-changed-pixels','pixels-total-in-original','changed-pixels-as-percent-of-pixels-in-original','number-of-pixels-in-bigger-image','changed-pixels-as-percent-of-pixels-in-bigger-image','count','shifts total','shiftsToTop','shiftsToTopRight','shiftsToRight','shiftsToBottomRight','shiftsToBottom','shiftsToBottomLeft','shiftsToLeft','shiftsToTopLeft','small shifts total','small shiftsToTop','small shiftsToTopRight','small shiftsToRight','small shiftsToBottomRight','small shiftsToBottom','small shiftsToBottomLeft','small shiftsToLeft','small shiftsToTopLeft','big shifts total','big shiftsToTop','big shiftsToTopRight','big shiftsToRight','big shiftsToBottomRight','big shiftsToBottom','big shiftsToBottomLeft','big shiftsToLeft','big shiftsToTopLeft']
    #reconstructionFeatures_grouped = [['pixels added through insertion/deletion','total number of changed pixels','pixels total in original','changed pixels as percent of pixels in original','number of pixels in bigger image','changed pixels as percent of pixels in bigger image'],['insertion-deletion-error','total-number-of-changed-pixels','pixels-total-in-original','changed-pixels-as-percent-of-pixels-in-original','number-of-pixels-in-bigger-image','changed-pixels-as-percent-of-pixels-in-bigger-image'], ['count'], ['shifts total','shiftsToTop','shiftsToTopRight','shiftsToRight','shiftsToBottomRight','shiftsToBottom','shiftsToBottomLeft','shiftsToLeft','shiftsToTopLeft'], ['small shifts total','small shiftsToTop','small shiftsToTopRight','small shiftsToRight','small shiftsToBottomRight','small shiftsToBottom','small shiftsToBottomLeft','small shiftsToLeft','small shiftsToTopLeft'],['big shifts total','big shiftsToTop','big shiftsToTopRight','big shiftsToRight','big shiftsToBottomRight','big shiftsToBottom','big shiftsToBottomLeft','big shiftsToLeft','big shiftsToTopLeft']]
    bestReconstructionFeatures_accuracy = ['total-number-of-changed-pixels']
    bestReconstructionFeatures_r2 = ['pixels added through insertion/deletion','total number of changed pixels','pixels total in original','changed pixels as percent of pixels in original','number of pixels in bigger image','changed pixels as percent of pixels in bigger image', 'insertion-deletion-error','total-number-of-changed-pixels','pixels-total-in-original','changed-pixels-as-percent-of-pixels-in-original','number-of-pixels-in-bigger-image','changed-pixels-as-percent-of-pixels-in-bigger-image', 'count', 'shifts total','shiftsToTop','shiftsToTopRight','shiftsToRight','shiftsToBottomRight','shiftsToBottom','shiftsToBottomLeft','shiftsToLeft','shiftsToTopLeft']
    reconstructionFeatures =  ['changed-pixels-as-percent-of-pixels-in-original', 'changed-pixels-as-percent-of-pixels-in-bigger-image']#bestReconstructionFeatures_accuracy#allBaselineFeatures + reconstructionFeatures_pure

    #allSubsetsOfBaselineFeatures = getSubsetOfFeatures(allBaselineFeatures, -1)
    #allSubsetsOfPureReconstructionFeatures = getSubsetOfFeatures(reconstructionFeatures, -1) #-1 for entire powerset

    imageColorAnalyzerFeatures = ['percentage complete from original','percentage missing from original']#['percentage complete from original']#, 'percentage missing from original', 'percentage additional from original']#['number of pixels in original','complete', 'additional','missing','percentage complete from original']

    #compute linear regression with all combinations of baselinefeature
    #for i in range(1, len(allSubsetsOfPureReconstructionFeatures)): #len(allSubsetsOfBaselineFeatures)):
    for i in range(0,1):
        #print(str(i) + " of " + str(len(allSubsetsOfPureReconstructionFeatures)))
        useBaselineFeatures = bestBaselineFeatures_accuracy #allBaselineFeatures #list(allSubsetsOfBaselineFeatures[i])
        reconstructionFeatures = bestReconstructionFeatures_accuracy#useBaselineFeatures + reconstructionFeatures
        #reconstructionFeatures = list(allSubsetsOfPureReconstructionFeatures[i])
        #reconstructionFeatures = ['\',\''.join(ele) for ele in reconstructionFeatures]
        #reconstructionFeatures = [item for sublist in reconstructionFeatures for item in sublist]
        #print(reconstructionFeatures)

        meanAbsoluteErrors_Baseline = [0] * 10
        meanAbsoluteErrors_Reconstruction = [0] * 10
        pearsons_rsBaseline = [0] * 10
        pearsons_rsReconstruction = [0] * 10
        r2s_Baseline = [0] * 10
        r2s_Reconstruction = [0] * 10
        accuracies_Baseline = [0] * 10
        accuracies_Reconstruction = [0] * 10

        confusionMatrices_Baseline = [0] * 10
        confusionMatrices_Reconstruction = [0] * 10

        predicted_y_baseline_test_all_folds = []
        predicted_y_reconstruction_test_all_folds = []
        test_y_all = []

        # for all folds
        for i in range(0,10):
            trainXy = getTrainTestDataFromFolds(stratify, i, True, False) # trainXy is a dataframe
            testXy = getTrainTestDataFromFolds(stratify, i, False, multipleSizeClasses) # testXy is a dataframe

            train_y = trainXy['quality-score']
            if(multipleSizeClasses):
                test_y = testXy[sizeClass]['quality-score']
                test_scaledX = scaleX(testXy[sizeClass], False)
            else:
                test_y = testXy['quality-score']
                test_scaledX = scaleX(testXy, False)

            test_y_all.extend(test_y)

            train_scaledX = scaleX(trainXy, False)

            train_baselineX = pandas.DataFrame(train_scaledX[useBaselineFeatures])
            test_baselineX = pandas.DataFrame(test_scaledX[useBaselineFeatures])

            predicted_y_baseline_test = performBaselineRegression(rounding_method, train_baselineX, train_y, test_baselineX, test_y) # returns predicted_y for testset
            predicted_y_baseline_test_all_folds.extend(predicted_y_baseline_test)
            #reduced_predicted_y = []
            #reduced_true_y = []
            #removeHits(test_y, predicted_y_baseline_test, reduced_true_y,reduced_predicted_y)
            #meanAbsoluteErrors_Baseline[i] = mean_absolute_error(reduced_predicted_y, reduced_true_y) # computes mean absolute error on unrounded predicted_y on test data wrt true test_y
            meanAbsoluteErrors_Baseline[i] = mean_absolute_error(performBaselineRegression(rounding_method, train_baselineX, train_y, test_baselineX, test_y), test_y) # computes mean absolute error on unrounded predicted_y on test data wrt true test_y
            pearsons_rsBaseline[i] = s.r_regression(predicted_y_baseline_test.reshape(-1,1), test_y, center=True)[0]
            r2s_Baseline[i] = r2_score(test_y, predicted_y_baseline_test)
            accuracies_Baseline[i] = computeAccuracy(test_y, predicted_y_baseline_test)
            print(computeConfusionMatrix(test_y,predicted_y_baseline_test))
            confusionMatrices_Baseline[i] = computeConfusionMatrix(test_y,predicted_y_baseline_test)

            if(doReconstruction):
                train_reconstructionX = pandas.DataFrame(train_scaledX[reconstructionFeatures])
                test_reconstructionX = pandas.DataFrame(test_scaledX[reconstructionFeatures])
                predicted_y_reconstruction_test = performReconstructionRegression(rounding_method, train_reconstructionX, train_y, test_reconstructionX, test_y) # returns predicted_y
                predicted_y_reconstruction_test_all_folds.extend(predicted_y_reconstruction_test)
                reduced_predicted_y = []
                #reduced_true_y = []
                #removeHits(test_y, predicted_y_reconstruction_test, reduced_true_y,reduced_predicted_y)
                #meanAbsoluteErrors_Reconstruction[i] = mean_absolute_error(reduced_predicted_y, reduced_true_y)
                meanAbsoluteErrors_Reconstruction[i] = mean_absolute_error(performReconstructionRegression(rounding_method, train_reconstructionX, train_y, test_reconstructionX, test_y), test_y)
                pearsons_rsReconstruction[i] = s.r_regression(predicted_y_reconstruction_test.reshape(-1,1), test_y, center=True)[0]
                r2s_Reconstruction[i] = r2_score(test_y, predicted_y_reconstruction_test)
                accuracies_Reconstruction[i] = computeAccuracy(test_y, predicted_y_reconstruction_test)
                confusionMatrices_Reconstruction[i] = computeConfusionMatrix(test_y,predicted_y_reconstruction_test)

            if(plot):
                plot_y(predicted_y_baseline_test, test_y, i, "baseline") # x-axis = predicted, y-axis= true
                if(doReconstruction):
                    plot_y(predicted_y_reconstruction_test, test_y, i, "reconstruction")

        if(plot_all):
            #print(str(predicted_y_baseline_test_all_folds))
            plot_y(predicted_y_baseline_test_all_folds, test_y_all, 10, "baseline")
            if(doReconstruction):
                plot_y(predicted_y_reconstruction_test_all_folds, test_y_all, 10, "reconstruction")

        meanAbsoluteError_Baseline = statistics.mean(meanAbsoluteErrors_Baseline)
        meanAbsoluteError_Reconstruction = statistics.mean(meanAbsoluteErrors_Reconstruction)
        pearsons_rBaseline = statistics.mean(pearsons_rsBaseline)
        pearsons_rReconstruction = statistics.mean(pearsons_rsReconstruction)
        r2_Baseline = statistics.mean(r2s_Baseline)
        r2_Reconstruction = statistics.mean(r2s_Reconstruction)
        accuracy_Baseline = statistics.mean(accuracies_Baseline)
        accuracy_Reconstruction = statistics.mean(accuracies_Reconstruction)
        confusionMatrix_Baseline = confusionMatrices_Baseline[0]
        confusionMatrix_Reconstruction = confusionMatrices_Reconstruction[0]

        for i in range(1, 10):
            confusionMatrix_Baseline += confusionMatrices_Baseline[i]
            confusionMatrix_Reconstruction += confusionMatrices_Reconstruction[i]
        #confusionMatrix_Baseline = confusionMatrix_Baseline / 10
        #confusionMatrix_Reconstruction = confusionMatrix_Reconstruction / 10
        print(str(confusionMatrix_Baseline))
        print(str(confusionMatrix_Reconstruction))

        result = [str(stratify), str(scale), str(rounding_method), str(reconstructionFeatures), str(r2_Baseline), str(r2_Reconstruction), str(accuracy_Baseline), str(accuracy_Reconstruction)]
        results.append(result)
        printResult(result, doReconstruction, r2s_Baseline, r2s_Reconstruction, accuracies_Baseline, accuracies_Reconstruction)
        print("--------------------MEAN ABSOLUTE ERROR ---------------------")
        print("MAE-Baseline: "+ str(meanAbsoluteErrors_Baseline))
        print("Mean MAE Baseline: " + str(meanAbsoluteError_Baseline))
        print("MAE-Reconstruction: "+ str(meanAbsoluteErrors_Reconstruction))
        print("Mean MAE Reconstruction: " + str(meanAbsoluteError_Reconstruction))
        print("--------------------PEARSON'S R ---------------------")
        print("(predicted_y,true_y) from all testsets")
        print("Pearson's r Baseline: "+str(pearsons_rsBaseline))
        print("Pearson's r Baseline Mean: "+str(pearsons_rBaseline))
        print("Pearson's r Reconstruction: "+str(pearsons_rsReconstruction))
        print("Pearson's r Reconstruction Mean: "+str(pearsons_rReconstruction))

    resultsDf = pandas.DataFrame(data=results)
    resultsDf.to_csv("resultsDf-image-color-analyzer.csv", index=False)

if __name__ == "__main__":
    main()
