package cli;

import java.util.*;

/**
 * Solution class with improved visualization showing primary piece next to exit
 */
public class Solution {
    private List<Move> moves;
    private List<Board> states;
    private int statesExamined;
    
    public Solution(List<Move> moves, List<Board> states, int statesExamined) {
        this.moves = moves;
        this.states = states;
        this.statesExamined = statesExamined;
    }
    
    public List<Move> getMoves() { return moves; }
    public List<Board> getStates() { return states; }
    public int getStatesExamined() { return statesExamined; }
    
    /**
     * Display the solution step by step with primary piece shown next to exit
     */
    public void displaySolution() {
        System.out.println("\nPapan Awal");
        states.get(0).display();
        
        // Display all moves except the last one
        for (int i = 0; i < moves.size() - 1; i++) {
            Move move = moves.get(i);
            System.out.println("\nGerakan " + (i + 1) + ": " + move.toString());
            
            Board nextState = states.get(i + 1);
            
            // If it's a compound move, highlight the moving piece
            if (move instanceof CompoundMove) {
                nextState.displayWithMove(move);
            } else {
                nextState.display();
            }
        }
        
        // Display final move with P shown outside next to exit
        if (moves.size() > 0) {
            int lastIndex = moves.size() - 1;
            Move lastMove = moves.get(lastIndex);
            System.out.println("\nGerakan " + (lastIndex + 1) + ": " + lastMove.toString());
            
            // Get final state
            Board finalState = states.get(states.size() - 1);
            
            // Use the visualization showing P next to K
            finalState.displayWithExitedPiece();
        }
        
        System.out.println("\n[Primary piece has reached the exit!]");
        
        // Display the complete move sequence
        System.out.println("\nMove sequence:");
        displayMoveSequence();
    }
    
    /**
     * Display the move sequence in a compact format
     */
    private void displayMoveSequence() {
        StringBuilder sequence = new StringBuilder();
        int count = 0;
        
        for (Move move : moves) {
            sequence.append(move.toString()).append(" ");
            count++;
            
            // Add newline every 16 moves for readability
            if (count % 16 == 0) {
                sequence.append("\n");
            }
        }
        
        sequence.append("(").append(moves.size()).append(" moves)");
        System.out.println(sequence.toString());
    }
}