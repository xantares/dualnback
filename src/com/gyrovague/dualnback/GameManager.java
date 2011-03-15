/**
 * Tracks the state of the game and determines what visuals/sounds to produce, the current
 * difficulty, game statistics.
 * 
 * Keep in mind that the main activity can be interrupted at any time, and an interruption
 * constitutes "losing" the current difficulty but doesn't mean the user gets penalised
 * otherwise.
 * 
 * Also note that the original study methodology is vague about what the user response should
 * be when both visual and auditory targets are present.  In my implementation I assume that
 * a distinct, third response is required (i.e. the possible user responses are four-fold:
 * no repetition, audio repetition, visual repetition, and both audio and visual repetition).
 * 
 * Reference: "Improving fluid intelligence with training on working memory",
 * PNAS May 13, 2008 vol. 105 no. 19 6829-6833.  Here is the most
 * useful excerpt:
 * 
 * "Materials. Training task. For the training task, we used the same material as described by Jaeggi et al. (33),
 * which was a dual n-back task where squares at eight different locations were presented sequentially on a computer
 * screen at a rate of 3 s (stimulus length, 500 ms; interstimulus interval, 2,500 ms). Simultaneously with the
 * presentation of the squares, one of eight consonants was presented sequentially through headphones. A response
 * was required whenever one of the presented stimuli matched the one presented n positions back in the sequence.
 * The value of n was the same for both streams of stimuli. There were six auditory and six visual targets per block
 * (four appearing in only one modality, and two appearing in both modalities simultaneously), and their positions were
 * determined randomly. Participants made responses manually by pressing on the letter �A� of a standard keyboard with
 * their left index finger for visual targets, and on the letter �L� with their right index finger for auditory targets.
 * No responses were required for non-targets.
 * 
 * In this task, the level of difficulty was varied by changing the level of n (34), which we used to track the
 * participants' performance. After each block, the participants' individual performance was analyzed, and in the
 * following block, the level of n was adapted accordingly: If the participant made fewer than three mistakes per
 * modality, the level of n increased by 1. It was decreased by 1 if more than five mistakes were made, and in all
 * other cases, n remained unchanged.
 * 
 * One training session comprised 20 blocks consisting of 20 + n trials resulting in a daily training time of �25 min."
 */
package com.gyrovague.dualnback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.os.Handler;
import android.util.Log;
import ec.util.MersenneTwister;

/**
 * @author asimihsan
 *
 */
public class GameManager {
	private static final String TAG = "GameManager";
	private int mNInterval = 1;
	private int mCurrentBlock;
	private int mCurrentTrial;
	private int mCurrentVisualMistakes;
	private int mCurrentAudioMistakes;
	private MersenneTwister mRNG;
	private ArrayList<Integer> mHistoryVisual;
	private ArrayList<Integer> mHistoryAudio;
	private Handler mHandlerUI;
	
	private static final int LOWER_MISTAKE_LIMIT = 3;
	private static final int UPPER_MISTAKE_LIMIT = 5;
	private static final int BLOCK_SIZE = 20;
	private static final int AUDIO_TARGETS_PER_BLOCK = 6;
	private static final int VISUAL_TARGETS_PER_BLOCK = 6;
	private static final int BOTH_MODES_PER_BLOCK = 2;
	private static final int NON_TARGETS_PER_BLOCK = BLOCK_SIZE - AUDIO_TARGETS_PER_BLOCK - VISUAL_TARGETS_PER_BLOCK + BOTH_MODES_PER_BLOCK;
	private static final int NUM_SQUARES = 8;
	private static final int NUM_CONSONANTS = 8;
	private static final int BLOCKS_PER_DAY = 20;
	
	GameManager(MersenneTwister mRNG) {
		this.mRNG = mRNG;
		commonConstructor();
	}
	
	GameManager(MersenneTwister RNG, int nInterval) {
		this.mRNG = RNG;
		this.mNInterval = nInterval;
		commonConstructor();
	}
	
	private void commonConstructor() {
		reset();
	}
	
	private void reset() {
		mCurrentBlock = 0;
	}
	
	public Trial getCurrentTrial() {
		int current_audio = mHistoryAudio.get(mCurrentTrial);
		int current_visual = mHistoryVisual.get(mCurrentTrial);
		boolean guessable = (mCurrentTrial > (mNInterval - 1)) ? true : false;                                    
		return new Trial(current_audio, current_visual, guessable);
	}
	
	private int getCurrentCorrectAnswer() {
		int previous_audio = mHistoryAudio.get(mCurrentTrial - mNInterval);
		int previous_visual = mHistoryVisual.get(mCurrentTrial - mNInterval);
		int current_audio = mHistoryAudio.get(mCurrentTrial);
		int current_visual = mHistoryVisual.get(mCurrentTrial);
		int correct_answer;
		
		if ((current_visual == previous_visual) && (current_audio == previous_audio)) {
			correct_answer = Guess.BOTH;
		} else if (current_visual == previous_visual) {
			correct_answer = Guess.VISUAL_ONLY;
		} else if (current_audio == previous_audio) {
			correct_answer = Guess.AUDIO_ONLY;
		} else {
			correct_answer = Guess.NO_REPETITION;
		}
		
		return correct_answer;
		
	} // private int getCurrentCorrectAnswer()
	
	public boolean evaluateGuess(int guess) {
		final String SUB_TAG = "::evaluateGuess()";
		Log.d(TAG + SUB_TAG, "entry.  guess: " + guess);
		boolean guessable = (mCurrentTrial > (mNInterval - 1)) ? true : false;
		Log.d(TAG + SUB_TAG, "mCurrentTrial: " + mCurrentTrial + ", mNInterval: " + mNInterval + ", guessable: " + guessable);
		if (!guessable) {
			mCurrentTrial++;
			return true;
		}

		int correct_answer = getCurrentCorrectAnswer();		
		boolean is_guess_correct = (guess == correct_answer); 
		if (!is_guess_correct) {
			switch (correct_answer) {
			    case Guess.BOTH:
			    	mCurrentVisualMistakes++;
			    	mCurrentAudioMistakes++;
			    	break;
			    case Guess.VISUAL_ONLY:
			    	mCurrentVisualMistakes++;
			    	break;
			    case Guess.AUDIO_ONLY:
			    	mCurrentAudioMistakes++;
			    	break;
			    case Guess.NO_REPETITION:
			    	switch (guess) {
			    		case Guess.BOTH:
			    			mCurrentVisualMistakes++;
					    	mCurrentAudioMistakes++;
			    			break;
			    		case Guess.AUDIO_ONLY:
			    			mCurrentAudioMistakes++;
			    			break;
			    		case Guess.VISUAL_ONLY:
			    			mCurrentVisualMistakes++;
			    			break;
			    	} // switch (guess)
			    	break;
			} // switch (correct_answer)
		} // if (!is_guess_correct)
		
		mCurrentTrial++;
		return is_guess_correct;
	}
	
	public void advanceBlock() {
		if ((mCurrentVisualMistakes < LOWER_MISTAKE_LIMIT) && (mCurrentAudioMistakes < LOWER_MISTAKE_LIMIT)) {
			mNInterval++;
		} else if ((mCurrentVisualMistakes > UPPER_MISTAKE_LIMIT) && (mCurrentAudioMistakes > UPPER_MISTAKE_LIMIT)) {
			mNInterval = Math.max(mNInterval - 1, 1);
		}
		prepareCurrentBlock();
	} // public void advanceBlock()
	
	public boolean isCurrentBlockFinished() {
		return (mCurrentTrial >= (BLOCK_SIZE + mNInterval));
	}
	
	public boolean isCurrentDayFinished() {
		return (mCurrentBlock >= (BLOCKS_PER_DAY));
	}
	
	public void prepareCurrentBlock() {
		mCurrentTrial = 0;
		mCurrentVisualMistakes = 0;
		mCurrentAudioMistakes = 0;
		mHistoryVisual = new ArrayList<Integer>(BLOCK_SIZE);
		mHistoryAudio = new ArrayList<Integer>(BLOCK_SIZE);		
		
		int numberNonTargets = NON_TARGETS_PER_BLOCK;
		int numberAudioTargets = AUDIO_TARGETS_PER_BLOCK - BOTH_MODES_PER_BLOCK;
		int numberVisualTargets = VISUAL_TARGETS_PER_BLOCK - BOTH_MODES_PER_BLOCK;
		int numberBothTargets = BOTH_MODES_PER_BLOCK;
		int marker = 0;
		mHistoryVisual.clear();
		mHistoryAudio.clear();
		
		// the first N entries will be completely random, since there can be
		// no repetitions.
		while (marker <= mNInterval) {
			mHistoryVisual.add(mRNG.nextInt(NUM_SQUARES));
			mHistoryAudio.add(mRNG.nextInt(NUM_CONSONANTS));
			marker += 1;
		}
		
		final int NON_TARGET = 0;
		final int AUDIO_TARGET = 1;
		final int VISUAL_TARGET = 2;
		final int BOTH_TARGETS = 3;
		Set<Integer> target_types = new HashSet<Integer>(Arrays.asList(new Integer[]{NON_TARGET, AUDIO_TARGET, VISUAL_TARGET, BOTH_TARGETS}));
		Integer target_types_list[] = new Integer[target_types.size()];
		target_types_list = target_types.toArray(target_types_list);
		boolean choice_invalid;
		final int limit = BLOCK_SIZE + mNInterval;
		
		while (marker <= limit) {
			choice_invalid = false;
			int previous_visual = mHistoryVisual.get(marker - mNInterval);
			int previous_audio = mHistoryAudio.get(marker - mNInterval);
			int choice = target_types_list[mRNG.nextInt(target_types_list.length)]; 
			switch(choice) {
				case NON_TARGET:
					// non-target
					if (numberNonTargets > 0) {
						mHistoryVisual.add(nextIntExceptN(NUM_SQUARES, previous_visual));
						mHistoryAudio.add(nextIntExceptN(NUM_CONSONANTS, previous_audio));
						numberNonTargets--;
						marker += 1;
					}
					if (numberNonTargets <= 0) {
						choice_invalid = true;
					}
					
					break;
					
				case AUDIO_TARGET:
					// audio-only target
					if (numberAudioTargets > 0) {
						mHistoryVisual.add(nextIntExceptN(NUM_SQUARES, previous_visual));
						mHistoryAudio.add(previous_audio);
						numberAudioTargets--;
						marker += 1;
					}
					if (numberAudioTargets <= 0) {
						choice_invalid = true;
					}
					break;
					
				case VISUAL_TARGET:
					// visual-only target
					if (numberVisualTargets > 0) {
						mHistoryVisual.add(previous_visual);
						mHistoryAudio.add(nextIntExceptN(NUM_CONSONANTS, previous_audio));
						numberVisualTargets--;
						marker += 1;
					}
					if (numberVisualTargets <= 0) {
						choice_invalid = true;
					}
					break;
					
				case BOTH_TARGETS:
					// both target
					if (numberBothTargets > 0) {
						mHistoryVisual.add(previous_visual);
						mHistoryAudio.add(previous_audio);
						numberBothTargets--;
						marker += 1;
					}
					if (numberBothTargets <= 0) {
						choice_invalid = true;
					}
					break;
			}
			
			if (choice_invalid && target_types.contains(choice)) {
				target_types.remove(choice);
				target_types_list = new Integer[target_types.size()];
				target_types_list = target_types.toArray(target_types_list);
			} // if (choice_invalid && target_types.contains(choice))
		} // while (marker <= limit)
		
	} // private void prepareCurrentBlock()
	
	private int nextIntExceptN(int limit, int n) {
		int result = n;
		while (result == n) {
			result = mRNG.nextInt(limit);
		}
		return result;
	}
	
	public int getnInterval() {
		return mNInterval;
	}
	
	public void setnInterval(int nInterval) {
		this.mNInterval = nInterval;
	}

	public void setmHandlerUI(Handler mHandlerUI) {
		this.mHandlerUI = mHandlerUI;
	}

}