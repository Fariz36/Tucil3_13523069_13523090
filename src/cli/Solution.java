package cli;

import java.util.*;

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
    
    public void displaySolution() {
        System.out.println("\nPapan Awal");
        states.get(0).display();
        
        for (int i = 0; i < moves.size(); i++) {
            System.out.println("\nGerakan " + (i + 1) + ": " + moves.get(i));
            states.get(i + 1).displayWithMove(moves.get(i));
        }
        
        System.out.println("\n[Primary piece has reached the exit!]");
    }
}
