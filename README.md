# Sudi: Part-of-Speech Tagger
The goal of this project is to build a personal digital assistant named "Sudi". 

To predict the correct part of speech tags for each sentence, this project uses the Hidden Markov Model (HMM) approach. In HMM, the states are the things we don't see (hidden) and are trying to infer, and the observations are what we do see. So the observations are words in a sentence and the states are tags because the text we'll observe is not annotated with its part of speech tag (that is the program's job). We proceed through a model by moving from state to state, producing one observation per state. In this "bigram" model, each tag depends on the previous tag. Then each word depends on the tag. 

An HMM is defined by its states (here part of speech tags), transitions (here tag to tag, with weights), and observations (here tag to word, with weights). Here is an HMM that includes tags, transitions, and observations for our example sentences. There is a special start state tag "#" before the start of the sentence. Note that we're not going to force a "stop" state (ending with a period, question mark, or exclamation point) since the texts we'll use from the Brown corpus include headlines that break that rule.

The Viterbi algorithm starts at the start state, with a score of 0, before any observation. Then to handle observation i, it propagates from each reached state at observation i-1, following each transition. The score for the next state as of observation i is the sum of the score at the current state as of i-1, plus the transition score from current to next, plus the score of observation i in the next. As with Dijkstra, that score may or may not be better than the score we already have for that state and observation — maybe we could get there from a different state with a better overall score. So we'll propagate forward from each current state to each next and check whether or not we've found something better (as with Dijkstra relax).

**Important**: We are not deciding for each word which tag is best. We are deciding for each POS tag that we reach for that word what its best score and preceding state are. This will ultimately yield the best path through the graph to generate the entire sentence, disambiguating the parts of speech of the various observed words along the way.

## Testing

To assess how good the model is, we implemented two kind of tests:
- Test using Brown corpus dataset. First we trained the machine using Brown corpus tags and sentences before performing the test. We also wrote an `accuracyTest` method that counts the number of correct and incorrect tags the model returns by comparing the result from the model with the correct tags passed by the user. For our test case on the Brown corpus dataset, we obtained 35109 tags right and 1285 tags wrong (~96% accuracy).  
- Console-based test: this is an interactive testing environment where a user can pass an input through the console. The model, then, predicts the correct tags for the sentences. 

## Usage 
Create a new model (object)
```java
HMM sudi = new HMM();
```

Train the model
```java
sudi.train("tagsPathName", "sentencesPathName");
```
where:
- `tagsPathName` is location of the tags file 
- `sentencesPathName` is location of the sentences file

(Optional)
If you want to compare the result produced by the model with the correct tags, follow the following format:
```java
sudi.accuracyTest("testTagPath", "testSentencePath");
```
where:
- `testTagPath` is location of the test tags file 
- `testSentencePath` is location of the test sentences file
## Assumptions
- "#" is the tag "before" the start of each sentence.

To start interactive console test:
```java
consoleViterbiTest();
```

To close the console test, send `exit` on the command line:
```java
exit
```

## Conclusion 
Overall, our test cases showed that the machine, while not perfect, has been trained enough to begin to recognize and draw conclusions about words and their parts of speech when given a sentence. We observed that the accuracy of the machine increases when the test cases are similar to the words we used for training it. 

This project was done as an assignment for Dartmouth’s Computer Science course. If you are a professor teaching this course and would like me to make the repository private, please reach out to me [here](mailto:aimen.a.abdulaziz.25@dartmouth.edu). Thanks!
