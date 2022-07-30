import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * The goal of this project is to build a model that labels each word in a phrase with its part-of-speech using
 * the Hidden Markov Model and Viterbi algorithm.
 * @author Aimen Abdulaziz, Winter 2022
 */

public class HMM {
	Map<String, Map<String,Double>> transitions;  // pos (current state) -> (pos (next state) -> transition probability)
	Map<String, Map<String, Double>> observations;// pos -> (word -> frequency)
	String start = "#";

	/**
	 * Constructor for Hidden Markov Model
	 */
	HMM() {
		transitions = new HashMap<String, Map<String, Double>>();
		observations = new HashMap<String, Map<String, Double>>();
	}

	/**
	 * Train both part of speech transition and words observation
	 * @param tagsPathName location of the tags file
	 * @param sentencesPathName location of the sentences file
	 */
	public void train(String tagsPathName, String sentencesPathName) {
		// open files
		BufferedReader tagsReader = openFile(tagsPathName);
		BufferedReader sentenceReader = openFile(sentencesPathName);

		// read the files
		try {
			// iterate through the training data to count the number of times each trans. and observation is observed
			String tagSentence;
			String wordSentence;
			while ((tagSentence = tagsReader.readLine()) != null && (wordSentence = sentenceReader.readLine()) != null) {
				// split tags by white space
				String[] tags = tagSentence.split(" ");

				// convert the words to lower case and split the sentences by white space
				String[] words = wordSentence.toLowerCase().split(" ");

				String currentTag;
				for (int i = 0; i < words.length; i++) {
					// if i = 0 (i.e beginning of the sentence), currentTag = start; else currentTag = tags[i-1]
					currentTag = (i == 0 ? start : tags[i - 1]);
					// add to the transitions map
					addToMap(transitions, currentTag, tags[i]);
					// add to the observations map
					addToMap(observations, tags[i], words[i]);
				}
			}
		}
		catch (IOException e) {
			System.err.println("I/O Error\n" + e.getMessage());
		}
		finally {
			// close files
			closeFile(tagsReader);
			closeFile(sentenceReader);

			// normalize counts for transitions and observations maps
			normalizeCounts(transitions);
			normalizeCounts(observations);
		}
	}

	/**
	 * Open the file in the specified pathName and return a BufferedReader
	 * @param pathName directory of file
	 * @return BufferedReader of the opened file
	 */
	public BufferedReader openFile(String pathName) {
		BufferedReader reader = null;
		// open the file, if possible
		try {
			reader = new BufferedReader(new FileReader(pathName));
		}
		catch (FileNotFoundException e) {
			System.err.println("Cannot open file.\n" + e.getMessage());
		}
		return reader;
	}

	/**
	 * Close the BufferedReader passed as a parameter
	 * @param reader BufferedReader
	 */
	public void closeFile(BufferedReader reader) {
		// close the file, if possible
		try {
			reader.close();
		}
		catch (IOException e) {
			System.err.println("Cannot close file.\n" + e.getMessage());
		}
	}

	/**
	 * Iterate through the training data and count the number of times each tag, word is observed
	 * @param map a nested map with String key and Map<String, Double> value
	 * @param bigKey key for the outer map
	 * @param smallKey key for the inner map
	 */
	public void addToMap(Map<String, Map<String,Double>> map, String bigKey, String smallKey) {
		if (!map.containsKey(bigKey)) {
			map.put(bigKey, new HashMap<String, Double>());
		}
		else if (map.get(bigKey).containsKey(smallKey)) {
			map.get(bigKey).put(smallKey, map.get(bigKey).get(smallKey) + 1.0);
		}
		else {
			map.get(bigKey).put(smallKey, 1.0);
		}
	}

	/**
	 * Normalize each state's counts to probabilities by dividing the total count for the state
	 * @param map take the transition and observation maps separately to normalize their frequency
	 */
	public void normalizeCounts(Map<String, Map<String,Double>> map) {
		// big -> outer map
		for (String big : map.keySet()) {
			double cumulativeSum = 0.0;
			// small -> inner map
			for (String small : map.get(big).keySet()) {
				cumulativeSum += map.get(big).get(small);
			}
			for (String small : map.get(big).keySet()) {
				double count = map.get(big).get(small);
				map.get(big).put(small, Math.log(count / cumulativeSum));
			}
		}
	}

	/**
	 * Implement Viterbi algorithm
	 * Given a sentence, construct a backtrace arraylist of map; then return the pos tag using the backtrace arraylist
	 * @param sentence string input from user
	 * @return arraylist of string with the best pos computed by the AI
	 */
	public ArrayList<String> viterbi(String sentence) {
		Map<String, String> currStates = new HashMap<String, String>(); // to state -> from state
		currStates.put(start, null); // start -> came from null
		Map<String, Double> currScores = new HashMap<>(); // state -> (score: currScore + transitionScore + observationScore)
		currScores.put(start, 0.0); // start with initial score of 0

		double totalScore; // currScore + transitionScore + observationScore
		double unseen = -100.0; // observation score for previously unseen words
		ArrayList<Map<String,String>> backTrace = new ArrayList<Map<String,String>>(); // arraylist of backTraceMap
		String bestPOS = null;

		String[] words = sentence.toLowerCase().split(" "); // split by any type of punctuation
		for (int i = 0; i < words.length; i++) {
			Map<String, String> nextStates = new HashMap<String, String>(); // to next state -> from current state
			Map<String, Double> nextScores = new HashMap<String, Double>(); // nextStates -> their respective score
			Map<String,String> backTraceMap = new HashMap<String,String>(); // nextState -> currentState
			Double bestScoreValue = Double.NEGATIVE_INFINITY; // smallest positive nonzero value

			for (String currState : currStates.keySet()) { // loop through all currStates; initially only #(start)
				for (String nextState : transitions.get(currState).keySet()) { // loop through all possible paths from currState
					nextStates.put(nextState, currState);

					// calculate totalScore by adding currScore + transScore + obsScore
					// if the word hasn't been observed before, use the unseen penalty for obsScore
					totalScore = currScores.get(currState) + transitions.get(currState).get(nextState)
							+ observations.get(nextState).getOrDefault(words[i], unseen);

					// add nextState if it doesn't have nextScore or update the score if the new score is better
					if (!nextScores.containsKey(nextState) || totalScore > nextScores.get(nextState)) {
						nextScores.put(nextState, totalScore);
						backTraceMap.put(nextState, currState);

						// keep track of best value and the pos for the best value
						if (totalScore > bestScoreValue) {
							bestPOS = nextState;
							bestScoreValue = totalScore;
						}
					}
				}
			}
			currStates = nextStates;
			currScores = nextScores;
			backTrace.add(backTraceMap);
		}

		// find a path from start pos to end pos
		// start at end pos and work backward using backTrack to start pos.
		ArrayList<String> pos = new ArrayList<String>();
		pos.add(bestPOS); // add the pos with the best value
		for (int i = 0; i < backTrace.size()-1; i++) {
			bestPOS = backTrace.get(backTrace.size()-1-i).get(bestPOS); // get previous pos
			pos.add(0, bestPOS);
		}
		return pos;
	}

	/**
	 * Test the accuracy of the model
	 * Compare the tags read from the test file to the tags computed by the AI
	 * @param tagsPathName tag location of the test file
	 * @param sentencesPathName sentence location of the test file
	 */
	public void accuracyTest(String tagsPathName, String sentencesPathName) {
		// open files
		BufferedReader tagsReader = openFile(tagsPathName);
		BufferedReader sentenceReader = openFile(sentencesPathName);

		// read the files
		try {
			ArrayList<String[]> allTagsForComparison = new ArrayList<String[]>();
			String tagSentence;
			while ((tagSentence = tagsReader.readLine()) != null) { // file not empty
				String[] tags = tagSentence.split(" ");
				allTagsForComparison.add(tags);
			}

			int incorrect = 0; // keep track of incorrect tags
			int correct = 0; // keep track of correct tags

			String wordSentence;
			int j = 0;
			while ((wordSentence = sentenceReader.readLine()) != null) { // file not empty
				ArrayList<String> answer = viterbi(wordSentence);
				String[] tags = allTagsForComparison.get(j);
				for (int i = 0; i < tags.length; i++) {
					if (Objects.equals(tags[i], answer.get(i))){
						correct++; // same value, so increment correct
					}
					else {
						incorrect++;
					}
				}
				j++;
			}
			System.out.println("The solution got " + correct + " tags right and " + incorrect + " tags wrong.");
		}
		catch (IOException e) {
			System.out.println("I/O Error\n" + e.getMessage());
		}
		finally {
			// close files
			closeFile(tagsReader);
			closeFile(sentenceReader);
		}
	}

	/**
	 * User interactive POS tagger for an input line
	 * The model is trained on the Brown Corpus dataset
	 * User can close the console test by sending "exit"
	 */
	public static void consoleViterbiTest() {
		System.out.println("\nStarting console test...");
		Scanner input = new Scanner(System.in);
		HMM consoleTest = new HMM();
		consoleTest.train("PS5/texts/brown-train-tags.txt", "PS5/texts/brown-train-sentences.txt");
		System.out.println("Console test started");
		String answer;

		// accept user input until user sends "exit"
		while (true) {
			System.out.println("Please enter the test:");
			answer = input.nextLine();
			if (answer.equals("exit")){
				System.out.println("Ending console test...");
				return;
			}
			System.out.println(consoleTest.viterbi(answer));
		}
	}

	public static void main(String[] args) {
		// Test 1: accuracy checker
		HMM sudi = new HMM();
		// train the model with the following files
		sudi.train("PS5/texts/brown-train-tags.txt", "PS5/texts/brown-train-sentences.txt");
		System.out.println("Accuracy test output");
		sudi.accuracyTest("PS5/texts/brown-test-tags.txt", "PS5/texts/brown-test-sentences.txt");
		sudi.accuracyTest("PS5/texts/simple-test-tags.txt", "PS5/texts/simple-test-sentences.txt");

		// Test 2: check user input sentences and print the tags
		consoleViterbiTest();
	}
}