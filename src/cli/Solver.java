package cli;

import java.util.*;

public class Solver {
    
    // UCS Implementation
    public Solution solveUCS(Board initialBoard) {
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Set<String> visited = new HashSet<>();
        int statesExamined = 0;
        
        Node startNode = new Node(initialBoard, null, null, 0);
        frontier.add(startNode);
        
        while (!frontier.isEmpty()) {
            Node current = frontier.poll();
            String stateString = current.board.getStateString();
            
            if (visited.contains(stateString)) {
                continue;
            }
            
            visited.add(stateString);
            statesExamined++;
            
            // Check if solved
            if (current.board.isSolved()) {
                return reconstructSolution(current, statesExamined);
            }
            
            // Generate possible moves
            List<Move> possibleMoves = current.board.getPossibleMoves();
            
            for (Move move : possibleMoves) {
                Board newBoard = current.board.makeMove(move);
                String newStateString = newBoard.getStateString();
                
                if (!visited.contains(newStateString)) {
                    int newCost = current.cost + 1;
                    Node newNode = new Node(newBoard, move, current, newCost);
                    frontier.add(newNode);
                }
            }
        }
        
        return null; // No solution found
    }
    
    // Dijkstra Implementation  
    public Solution solveDijkstra(Board initialBoard) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Map<String, Integer> distances = new HashMap<>();
        Map<String, Node> nodeMap = new HashMap<>();
        Set<String> processed = new HashSet<>();
        int statesExamined = 0;
        
        Node startNode = new Node(initialBoard, null, null, 0);
        String startState = initialBoard.getStateString();
        
        pq.add(startNode);
        distances.put(startState, 0);
        nodeMap.put(startState, startNode);
        
        while (!pq.isEmpty()) {
            Node current = pq.poll();
            String currentState = current.board.getStateString();
            
            if (processed.contains(currentState)) {
                continue;
            }
            
            processed.add(currentState);
            statesExamined++;
            
            // Check if solved
            if (current.board.isSolved()) {
                return reconstructSolution(current, statesExamined);
            }
            
            // Generate possible moves
            List<Move> possibleMoves = current.board.getPossibleMoves();
            
            for (Move move : possibleMoves) {
                Board newBoard = current.board.makeMove(move);
                String newStateString = newBoard.getStateString();
                
                if (!processed.contains(newStateString)) {
                    int newDistance = current.cost + 1;
                    
                    if (!distances.containsKey(newStateString) || newDistance < distances.get(newStateString)) {
                        distances.put(newStateString, newDistance);
                        Node newNode = new Node(newBoard, move, current, newDistance);
                        pq.add(newNode);
                        nodeMap.put(newStateString, newNode);
                    }
                }
            }
        }
        
        return null; // No solution found
    }
    
    // Greedy Best First Search Implementation
    public Solution solveGreedy(Board initialBoard, String heuristic) {
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.h));
        Set<String> visited = new HashSet<>();
        int statesExamined = 0;
        
        int h = calculateHeuristic(initialBoard, heuristic);
        Node startNode = new Node(initialBoard, null, null, 0, h, h);
        frontier.add(startNode);
        
        while (!frontier.isEmpty()) {
            Node current = frontier.poll();
            String stateString = current.board.getStateString();
            
            if (visited.contains(stateString)) {
                continue;
            }
            
            visited.add(stateString);
            statesExamined++;
            
            // Check if solved
            if (current.board.isSolved()) {
                return reconstructSolution(current, statesExamined);
            }
            
            // Generate possible moves
            List<Move> possibleMoves = current.board.getPossibleMoves();
            
            for (Move move : possibleMoves) {
                Board newBoard = current.board.makeMove(move);
                String newStateString = newBoard.getStateString();
                
                if (!visited.contains(newStateString)) {
                    int newH = calculateHeuristic(newBoard, heuristic);
                    Node newNode = new Node(newBoard, move, current, current.cost + 1, newH, newH);
                    frontier.add(newNode);
                }
            }
        }
        
        return null; // No solution found
    }
    
    // A* Search Implementation
    public Solution solveAStar(Board initialBoard, String heuristic) {
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Set<String> visited = new HashSet<>();
        Map<String, Node> nodeMap = new HashMap<>();
        int statesExamined = 0;
        
        int h = calculateHeuristic(initialBoard, heuristic);
        Node startNode = new Node(initialBoard, null, null, 0, h, h);
        frontier.add(startNode);
        nodeMap.put(initialBoard.getStateString(), startNode);
        
        while (!frontier.isEmpty()) {
            Node current = frontier.poll();
            String stateString = current.board.getStateString();
            
            if (visited.contains(stateString)) {
                continue;
            }
            
            visited.add(stateString);
            statesExamined++;
            
            // Check if solved
            if (current.board.isSolved()) {
                return reconstructSolution(current, statesExamined);
            }
            
            // Generate possible moves
            List<Move> possibleMoves = current.board.getPossibleMoves();
            
            for (Move move : possibleMoves) {
                Board newBoard = current.board.makeMove(move);
                String newStateString = newBoard.getStateString();
                
                if (!visited.contains(newStateString)) {
                    int newG = current.cost + 1;
                    int newH = calculateHeuristic(newBoard, heuristic);
                    int newF = newG + newH;
                    
                    if (!nodeMap.containsKey(newStateString) || nodeMap.get(newStateString).f > newF) {
                        Node newNode = new Node(newBoard, move, current, newG, newH, newF);
                        frontier.add(newNode);
                        nodeMap.put(newStateString, newNode);
                    }
                }
            }
        }
        
        return null; // No solution found
    }
    
    // Calculate heuristic value
    private int calculateHeuristic(Board board, String heuristic) {
        switch (heuristic.toLowerCase()) {
            case "manhattan distance":
            case "manhattan":
                return calculateManhattanDistance(board);
            case "direct distance":
            case "direct":
                return calculateDirectDistance(board);
            case "blocking count":
            case "blocking":
                return calculateBlockingCount(board);
            default:
                return calculateManhattanDistance(board);
        }
    }
    
    // Manhattan distance heuristic
    private int calculateManhattanDistance(Board board) {
        Piece primaryPiece = board.getPrimaryPiece();
        if (primaryPiece == null) return Integer.MAX_VALUE;
        
        Position exitPos = board.getExitPosition();
        if (exitPos == null) return Integer.MAX_VALUE;
        
        // Get the position of primary piece that's closest to exit
        int minDistance = Integer.MAX_VALUE;
        for (Position pos : primaryPiece.getPositions()) {
            int distance = Math.abs(pos.row - exitPos.row) + Math.abs(pos.col - exitPos.col);
            minDistance = Math.min(minDistance, distance);
        }
        
        return minDistance;
    }
    
    // Direct distance to exit (considering orientation)
    private int calculateDirectDistance(Board board) {
        Piece primaryPiece = board.getPrimaryPiece();
        if (primaryPiece == null) return Integer.MAX_VALUE;
        
        Position exitPos = board.getExitPosition();
        if (exitPos == null) return Integer.MAX_VALUE;
        
        if (primaryPiece.getOrientation() == Orientation.HORIZONTAL) {
            // For horizontal piece, only consider column distance
            Position rightmost = primaryPiece.getPositions().get(primaryPiece.getPositions().size() - 1);
            return Math.abs(rightmost.col - exitPos.col);
        } else {
            // For vertical piece, only consider row distance
            Position bottommost = primaryPiece.getPositions().get(primaryPiece.getPositions().size() - 1);
            return Math.abs(bottommost.row - exitPos.row);
        }
    }
    
    // Count pieces blocking the path to exit
    private int calculateBlockingCount(Board board) {
        Piece primaryPiece = board.getPrimaryPiece();
        if (primaryPiece == null) return Integer.MAX_VALUE;
        
        Position exitPos = board.getExitPosition();
        if (exitPos == null) return Integer.MAX_VALUE;
        
        int blockingCount = 0;
        
        // Get the path from primary piece to exit
        if (primaryPiece.getOrientation() == Orientation.HORIZONTAL) {
            int row = primaryPiece.getPositions().get(0).row;
            Position rightmost = primaryPiece.getPositions().get(primaryPiece.getPositions().size() - 1);
            
            // Check all positions between primary piece and exit
            int start = rightmost.col + 1;
            int end = board.getWidth();
            if (exitPos.col < rightmost.col) {
                start = 0;
                end = primaryPiece.getPositions().get(0).col;
            }
            
            for (int col = start; col < end; col++) {
                if (col >= 0 && col < board.getWidth() && row >= 0 && row < board.getHeight()) {
                    char cell = board.getGrid()[row][col];
                    if (cell != '.' && cell != 'P') {
                        blockingCount++;
                    }
                }
            }
        } else {
            // Vertical orientation
            int col = primaryPiece.getPositions().get(0).col;
            Position bottommost = primaryPiece.getPositions().get(primaryPiece.getPositions().size() - 1);
            
            // Check all positions between primary piece and exit
            int start = bottommost.row + 1;
            int end = board.getHeight();
            if (exitPos.row < bottommost.row) {
                start = 0;
                end = primaryPiece.getPositions().get(0).row;
            }
            
            for (int row = start; row < end; row++) {
                if (row >= 0 && row < board.getHeight() && col >= 0 && col < board.getWidth()) {
                    char cell = board.getGrid()[row][col];
                    if (cell != '.' && cell != 'P') {
                        blockingCount++;
                    }
                }
            }
        }
        
        return blockingCount;
    }
    
    // Reconstruct solution from final node
    private Solution reconstructSolution(Node goalNode, int statesExamined) {
        List<Move> moves = new ArrayList<>();
        List<Board> states = new ArrayList<>();
        
        Node current = goalNode;
        
        // Trace back from goal to start
        while (current != null) {
            states.add(0, current.board);
            if (current.move != null) {
                moves.add(0, current.move);
            }
            current = current.parent;
        }
        
        return new Solution(moves, states, statesExamined);
    }
    
    // Inner class representing a search node
    private static class Node {
        Board board;
        Move move;
        Node parent;
        int cost;   // g(n) - cost from start
        int h;      // h(n) - heuristic value
        int f;      // f(n) = g(n) + h(n)
        
        Node(Board board, Move move, Node parent, int cost) {
            this.board = board;
            this.move = move;
            this.parent = parent;
            this.cost = cost;
            this.h = 0;
            this.f = cost;
        }
        
        Node(Board board, Move move, Node parent, int cost, int h, int f) {
            this.board = board;
            this.move = move;
            this.parent = parent;
            this.cost = cost;
            this.h = h;
            this.f = f;
        }
    }
}