package cli;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Solver class implementing different pathfinding algorithms for the Rush Hour puzzle
 * with support for compound moves (multi-cell movements in one direction count as one move)
 * and tracking of examined node count
 */
public class Solver {
    
    // Added to track nodes examined even when no solution is found
    private int lastNodesExamined = 0;
    
    /**
     * Get the number of states examined in the last solving attempt
     */
    public int getLastNodesExamined() {
        return lastNodesExamined;
    }

    private static class Result {
        boolean found;
        Node node;
        int nextThreshold;

        Result(boolean found, Node node, int nextThreshold) {
            this.found = found;
            this.node = node;
            this.nextThreshold = nextThreshold;
        }
    }
    
    /**
     * UCS Implementation with compound moves
     */
    public Solution solveUCS(Board initialBoard, boolean isCompound) {
        System.out.println("Searching for solution using UCS");
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Set<String> visited = new HashSet<>();
        lastNodesExamined = 0; // Reset counter
        
        Node startNode = new Node(initialBoard, null, null, 0);
        frontier.add(startNode);
        
        while (!frontier.isEmpty()) {
            Node current = frontier.poll();
            String stateString = current.board.getStateString();
            
            if (visited.contains(stateString)) {
                continue;
            }
            
            visited.add(stateString);
            lastNodesExamined++; // Increment counter
            
            // Check if solved
            if (current.board.isSolved()) {
                return reconstructSolution(current, lastNodesExamined);
            }
            
            // Generate compound moves (multi-cell movements)
            List<CompoundMove> compoundMoves = generateCompoundMoves(current.board, isCompound);
            
            for (CompoundMove move : compoundMoves) {
                Board newBoard = makeCompoundMove(current.board, move);
                String newStateString = newBoard.getStateString();
                
                if (!visited.contains(newStateString)) {
                    int newCost = current.cost + 1; // Each compound move costs 1
                    Node newNode = new Node(newBoard, move, current, newCost);
                    frontier.add(newNode);
                }
            }
        }
        
        return null; // No solution found, but lastNodesExamined has been updated
    }
    
    /**
     * A* Search Implementation with compound moves
     */
    public Solution solveAStar(Board initialBoard, String heuristic, boolean isCompound) {
        System.out.println("Searching for solution using A* with heuristic: " + heuristic);
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Set<String> visited = new HashSet<>();
        Map<String, Node> nodeMap = new HashMap<>();
        lastNodesExamined = 0; // Reset counter
        
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
            lastNodesExamined++; // Increment counter
            
            // Check if solved
            if (current.board.isSolved()) {
                return reconstructSolution(current, lastNodesExamined);
            }
            
            // Generate compound moves (multi-cell movements)
            List<CompoundMove> compoundMoves = generateCompoundMoves(current.board, isCompound);
            
            for (CompoundMove move : compoundMoves) {
                Board newBoard = makeCompoundMove(current.board, move);
                String newStateString = newBoard.getStateString();
                
                if (!visited.contains(newStateString)) {
                    int newG = current.cost + 1; // Each compound move costs 1
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
        
        return null; // No solution found, but lastNodesExamined has been updated
    }
    
    /**
     * Greedy Best First Search Implementation with compound moves
     */
    public Solution solveGreedy(Board initialBoard, String heuristic, boolean isCompound) {
        System.out.println("Searching for solution using Greedy Best First Search with heuristic: " + heuristic);
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.h));
        Set<String> visited = new HashSet<>();
        lastNodesExamined = 0; // Reset counter
        
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
            lastNodesExamined++; // Increment counter
            
            // Check if solved
            if (current.board.isSolved()) {
                return reconstructSolution(current, lastNodesExamined);
            }
            
            // Generate compound moves (multi-cell movements)
            List<CompoundMove> compoundMoves = generateCompoundMoves(current.board, isCompound);
            
            for (CompoundMove move : compoundMoves) {
                Board newBoard = makeCompoundMove(current.board, move);
                String newStateString = newBoard.getStateString();
                
                if (!visited.contains(newStateString)) {
                    int newH = calculateHeuristic(newBoard, heuristic);
                    Node newNode = new Node(newBoard, move, current, current.cost + 1, newH, newH);
                    frontier.add(newNode);
                }
            }
        }
        
        return null; // No solution found, but lastNodesExamined has been updated
    }
    
    /**
     * Dijkstra's algorithm implementation - similar to UCS but with different node mapping
     */
    public Solution solveDijkstra(Board initialBoard, boolean isCompound) {
        System.out.println("Searching for solution using Dijkstra's algorithm");
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Set<String> visited = new HashSet<>();
        Map<String, Integer> costSoFar = new HashMap<>();
        lastNodesExamined = 0; // Reset counter
        
        Node startNode = new Node(initialBoard, null, null, 0);
        frontier.add(startNode);
        costSoFar.put(initialBoard.getStateString(), 0);
        
        while (!frontier.isEmpty()) {
            Node current = frontier.poll();
            String stateString = current.board.getStateString();
            
            if (visited.contains(stateString)) {
                continue;
            }
            
            visited.add(stateString);
            lastNodesExamined++; // Increment counter
            
            // Check if solved
            if (current.board.isSolved()) {
                return reconstructSolution(current, lastNodesExamined);
            }
            
            // Generate compound moves
            List<CompoundMove> compoundMoves = generateCompoundMoves(current.board, isCompound);
            
            for (CompoundMove move : compoundMoves) {
                Board newBoard = makeCompoundMove(current.board, move);
                String newStateString = newBoard.getStateString();
                
                // For Dijkstra, treat all moves as cost 1
                int newCost = current.cost + 1;
                
                if (!costSoFar.containsKey(newStateString) || newCost < costSoFar.get(newStateString)) {
                    costSoFar.put(newStateString, newCost);
                    Node newNode = new Node(newBoard, move, current, newCost);
                    frontier.add(newNode);
                }
            }
        }
        
        return null; // No solution found, but lastNodesExamined has been updated
    }

    public Solution solveBeam(Board initialBoard, String heuristic, boolean isCompound) {
        System.out.println("Searching for solution using Beam Search with heuristic: " + heuristic);
        int beamWidth = 50;
        List<Node> frontier = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        lastNodesExamined = 0;

        int h = calculateHeuristic(initialBoard, heuristic);
        Node startNode = new Node(initialBoard, null, null, 0, h, h);
        frontier.add(startNode);

        while (!frontier.isEmpty()) {
            List<Node> nextLevel = new ArrayList<>();

            for (Node current : frontier) {
                String stateString = current.board.getStateString();

                if (visited.contains(stateString)) {
                    continue;
                }

                visited.add(stateString);
                lastNodesExamined++;

                // Goal check
                if (current.board.isSolved()) {
                    return reconstructSolution(current, lastNodesExamined);
                }

                // Generate children
                List<CompoundMove> compoundMoves = generateCompoundMoves(current.board, isCompound);

                for (CompoundMove move : compoundMoves) {
                    Board newBoard = makeCompoundMove(current.board, move);
                    String newStateString = newBoard.getStateString();

                    if (!visited.contains(newStateString)) {
                        int newH = calculateHeuristic(newBoard, heuristic);
                        Node newNode = new Node(newBoard, move, current, current.cost + 1, newH, newH);
                        nextLevel.add(newNode);
                    }
                }
            }

            // Sort all next level nodes by heuristic value and select top beamWidth
            nextLevel.sort(Comparator.comparingInt(n -> n.h));
            frontier = nextLevel.subList(0, Math.min(beamWidth, nextLevel.size()));
        }

        return null; // No solution found
    }

    public Solution solveIDAStar(Board initialBoard, String heuristic, boolean isCompound) {
        // IDA* Search Implementation with compound moves
        System.out.println("Searchinig for solution using IDA* with heuristic: " + heuristic);
        lastNodesExamined = 0;

        int h = calculateHeuristic(initialBoard, heuristic);
        Node root = new Node(initialBoard, null, null, 0, h, h);
        int threshold = root.f;

        while (true) {
            Set<String> visited = new HashSet<>();
            Result result = dfsIDA(root, heuristic, isCompound, threshold, visited);
            if (result.found) {
                return reconstructSolution(result.node, lastNodesExamined);
            }
            if (result.nextThreshold == Integer.MAX_VALUE) {
                return null; // No solution
            }
            threshold = result.nextThreshold;
        }
    }

    private Result dfsIDA(Node current, String heuristic, boolean isCompound, int threshold, Set<String> visited) {
        lastNodesExamined++;
        String stateString = current.board.getStateString();
        if (visited.contains(stateString)) return new Result(false, null, Integer.MAX_VALUE);
        visited.add(stateString);

        int f = current.cost + current.h;
        if (f > threshold) return new Result(false, null, f);
        if (current.board.isSolved()) return new Result(true, current, f);

        int minThreshold = Integer.MAX_VALUE;

        for (CompoundMove move : generateCompoundMoves(current.board, isCompound)) {
            Board newBoard = makeCompoundMove(current.board, move);
            String newStateString = newBoard.getStateString();
            if (visited.contains(newStateString)) continue;

            int newH = calculateHeuristic(newBoard, heuristic);
            Node child = new Node(newBoard, move, current, current.cost + 1, newH, current.cost + 1 + newH);
            Result result = dfsIDA(child, heuristic, isCompound, threshold, visited);

            if (result.found) return result;
            minThreshold = Math.min(minThreshold, result.nextThreshold);
        }

        visited.remove(stateString);
        return new Result(false, null, minThreshold);
    }


    private List<CompoundMove> generateCompoundMoves(Board board) {
        return generateCompoundMoves(board, true); // default isCompound = true
    }
    
    /**
     * Generate all possible compound moves (multi-cell movements) for a board
     */
    private List<CompoundMove> generateCompoundMoves(Board board, boolean isCompound) {
        List<CompoundMove> compoundMoves = new ArrayList<>();
        
        if (isCompound) {
            for (Piece piece : board.getPieces()) {
            // Try all possible moves for each piece
                if (piece.getOrientation() == Orientation.HORIZONTAL) {
                    // Try moving right
                    int maxRight = findMaximumDistance(board, piece, "right");
                    if (maxRight > 0) {
                        compoundMoves.add(new CompoundMove(piece, "right", maxRight));
                    }
                    
                    // Try moving left
                    int maxLeft = findMaximumDistance(board, piece, "left");
                    if (maxLeft > 0) {
                        compoundMoves.add(new CompoundMove(piece, "left", maxLeft));
                    }
                } else {
                    // Try moving down
                    int maxDown = findMaximumDistance(board, piece, "down");
                    if (maxDown > 0) {
                        compoundMoves.add(new CompoundMove(piece, "down", maxDown));
                    }
                    
                    // Try moving up
                    int maxUp = findMaximumDistance(board, piece, "up");
                    if (maxUp > 0) {
                        compoundMoves.add(new CompoundMove(piece, "up", maxUp));
                    }
                }
            }
        }
        else {
            for (Piece piece : board.getPieces()) {
            // Try all possible moves for each piece
                if (piece.getOrientation() == Orientation.HORIZONTAL) {
                    // Try moving right
                    int maxRight = findMaximumDistance(board, piece, "right");
                    if (maxRight > 0) {
                        compoundMoves.add(new CompoundMove(piece, "right", 1));
                    }
                    
                    // Try moving left
                    int maxLeft = findMaximumDistance(board, piece, "left");
                    if (maxLeft > 0) {
                        compoundMoves.add(new CompoundMove(piece, "left", 1));
                    }
                } else {
                    // Try moving down
                    int maxDown = findMaximumDistance(board, piece, "down");
                    if (maxDown > 0) {
                        compoundMoves.add(new CompoundMove(piece, "down", 1));
                    }
                    
                    // Try moving up
                    int maxUp = findMaximumDistance(board, piece, "up");
                    if (maxUp > 0) {
                        compoundMoves.add(new CompoundMove(piece, "up", 1));
                    }
                }
            }
        }
        
        return compoundMoves;
    }
    
    /**
     * Find the maximum distance a piece can move in a given direction
     * Stop if primary piece reaches exit
     */
    private int findMaximumDistance(Board board, Piece piece, String direction) {
        int distance = 0;
        Board currentBoard = board;
        boolean isPrimaryPiece = piece.getId() == 'P';
        
        // Keep moving until we can't move anymore
        while (true) {
            // Check if we can move one more step
            List<Move> possibleMoves = currentBoard.getPossibleMoves();
            boolean canMove = false;
            
            for (Move move : possibleMoves) {
                if (move.getPiece().getId() == piece.getId() && move.getDirection().equals(direction)) {
                    canMove = true;
                    currentBoard = currentBoard.makeMove(move);
                    distance++;
                    
                    // Check if primary piece has reached exit
                    if (isPrimaryPiece && currentBoard.isSolved()) {
                        return distance; // Stop at exit for primary piece
                    }
                    
                    break;
                }
            }
            
            if (!canMove) {
                break;
            }
        }
        
        return distance;
    }
    
    /**
     * Apply a compound move to a board with proper exit detection
     */
    private Board makeCompoundMove(Board board, CompoundMove move) {
        Board currentBoard = board;
        String direction = move.getDirection();
        char pieceId = move.getPiece().getId();
        boolean isPrimaryPiece = pieceId == 'P';
        
        // Apply the move step by step
        for (int i = 0; i < move.getDistance(); i++) {
            // Find the piece in the current board
            Piece targetPiece = null;
            for (Piece p : currentBoard.getPieces()) {
                if (p.getId() == pieceId) {
                    targetPiece = p;
                    break;
                }
            }
            
            if (targetPiece == null) {
                break; // Piece not found (might happen if primary piece exits)
            }
            
            // Create a simple move and apply it
            Move simpleMove = new Move(targetPiece, direction);
            Board nextBoard = currentBoard.makeMove(simpleMove);
            
            // Check if the primary piece has reached the exit
            if (isPrimaryPiece && nextBoard.isSolved()) {
                return nextBoard; // Stop movement once primary piece reaches exit
            }
            
            currentBoard = nextBoard;
        }
        
        return currentBoard;
    }
    
    /**
     * Reconstruct solution from final node
     */
    private Solution reconstructSolution(Node goalNode, int statesExamined) {
        List<Move> moves = new ArrayList<>();
        List<Board> states = new ArrayList<>();
        
        Node current = goalNode;
        
        // Trace back from goal to start
        while (current != null) {
            if (current.board != null) {
                states.add(0, current.board);
            }
            if (current.move != null) {
                moves.add(0, current.move);
            }
            current = current.parent;
        }
        
        return new Solution(moves, states, statesExamined);
    }
    
    /**
     * Calculate heuristic value
     */
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
            case "clearing moves":
            case "clearing":
                return calculateClearingMoves(board);
            default:
                return calculateManhattanDistance(board);
        }
    }
    
    /**
     * Manhattan distance heuristic
     */
    private int calculateManhattanDistance(Board board) {
        Piece primaryPiece = board.getPrimaryPiece();
        if (primaryPiece == null) return Integer.MAX_VALUE;
        
        cli.Position exitPos = board.getExitPosition();
        if (exitPos == null) return Integer.MAX_VALUE;
        
        // Get the position of primary piece that's closest to exit
        int minDistance = Integer.MAX_VALUE;
        for (cli.Position pos : primaryPiece.getPositions()) {
            int distance = Math.abs(pos.row - exitPos.row) + Math.abs(pos.col - exitPos.col);
            minDistance = Math.min(minDistance, distance);
        }
        
        return minDistance;
    }
    
    /**
     * Direct distance to exit (considering orientation)
     */
    private int calculateDirectDistance(Board board) {
        Piece primaryPiece = board.getPrimaryPiece();
        if (primaryPiece == null) return Integer.MAX_VALUE;
        
        cli.Position exitPos = board.getExitPosition();
        if (exitPos == null) return Integer.MAX_VALUE;
        
        // We need to account for the orientation of the piece and the exit side
        Exit exitSide = board.getExitSide();
        
        if (primaryPiece.getOrientation() == Orientation.HORIZONTAL) {
            // For horizontal piece
            if (exitSide == Exit.RIGHT) {
                // Need to get rightmost position of piece
                cli.Position rightmost = primaryPiece.getPositions().stream()
                    .max(Comparator.comparingInt(p -> p.col))
                    .orElse(primaryPiece.getPositions().get(0));
                return board.getWidth() - rightmost.col - 1; // Distance to right edge
            } else if (exitSide == Exit.LEFT) {
                // Need to get leftmost position of piece
                cli.Position leftmost = primaryPiece.getPositions().stream()
                    .min(Comparator.comparingInt(p -> p.col))
                    .orElse(primaryPiece.getPositions().get(0));
                return leftmost.col; // Distance to left edge
            } else {
                // Exit is not aligned with piece orientation
                return Integer.MAX_VALUE;
            }
        } else {
            // For vertical piece
            if (exitSide == Exit.BOTTOM) {
                // Need to get bottommost position of piece
                cli.Position bottommost = primaryPiece.getPositions().stream()
                    .max(Comparator.comparingInt(p -> p.row))
                    .orElse(primaryPiece.getPositions().get(0));
                return board.getHeight() - bottommost.row - 1; // Distance to bottom edge
            } else if (exitSide == Exit.TOP) {
                // Need to get topmost position of piece
                cli.Position topmost = primaryPiece.getPositions().stream()
                    .min(Comparator.comparingInt(p -> p.row))
                    .orElse(primaryPiece.getPositions().get(0));
                return topmost.row; // Distance to top edge
            } else {
                // Exit is not aligned with piece orientation
                return Integer.MAX_VALUE;
            }
        }
    }
    
    /**
     * Count pieces blocking the path to exit
     */
    private int calculateBlockingCount(Board board) {
        Piece primaryPiece = board.getPrimaryPiece();
        if (primaryPiece == null) return Integer.MAX_VALUE;
        
        cli.Position exitPos = board.getExitPosition();
        if (exitPos == null) return Integer.MAX_VALUE;
        
        int blockingCount = 0;
        char[][] grid = board.getGrid();
        
        // Get the path from primary piece to exit
        if (primaryPiece.getOrientation() == Orientation.HORIZONTAL) {
            int row = primaryPiece.getPositions().get(0).row;
            
            // Find the range to check based on exit position
            int startCol, endCol;
            if (board.getExitSide() == Exit.RIGHT) {
                // Rightmost position of primary piece
                cli.Position rightmost = primaryPiece.getPositions().stream()
                    .max(Comparator.comparingInt(p -> p.col))
                    .orElse(primaryPiece.getPositions().get(0));
                
                startCol = rightmost.col + 1;
                endCol = board.getWidth();
            } else {
                // Leftmost position of primary piece
                cli.Position leftmost = primaryPiece.getPositions().stream()
                    .min(Comparator.comparingInt(p -> p.col))
                    .orElse(primaryPiece.getPositions().get(0));
                
                startCol = 0;
                endCol = leftmost.col;
            }
            
            // Check for blocking pieces
            Set<Character> blockingPieces = new HashSet<>();
            for (int col = startCol; col < endCol; col++) {
                if (row >= 0 && row < grid.length && col >= 0 && col < grid[row].length) {
                    char cell = grid[row][col];
                    if (cell != '.' && cell != 'P' && !blockingPieces.contains(cell)) {
                        blockingPieces.add(cell);
                        blockingCount++;
                    }
                }
            }
        } else {
            // Vertical orientation
            int col = primaryPiece.getPositions().get(0).col;
            
            // Find the range to check based on exit position
            int startRow, endRow;
            if (board.getExitSide() == Exit.BOTTOM) {
                // Bottommost position of primary piece
                cli.Position bottommost = primaryPiece.getPositions().stream()
                    .max(Comparator.comparingInt(p -> p.row))
                    .orElse(primaryPiece.getPositions().get(0));
                
                startRow = bottommost.row + 1;
                endRow = board.getHeight();
            } else {
                // Topmost position of primary piece
                cli.Position topmost = primaryPiece.getPositions().stream()
                    .min(Comparator.comparingInt(p -> p.row))
                    .orElse(primaryPiece.getPositions().get(0));
                
                startRow = 0;
                endRow = topmost.row;
            }
            
            // Check for blocking pieces
            Set<Character> blockingPieces = new HashSet<>();
            for (int row = startRow; row < endRow; row++) {
                if (row >= 0 && row < grid.length && col >= 0 && col < grid[row].length) {
                    char cell = grid[row][col];
                    if (cell != '.' && cell != 'P' && !blockingPieces.contains(cell)) {
                        blockingPieces.add(cell);
                        blockingCount++;
                    }
                }
            }
        }
        
        return blockingCount;
    }

    private int calculateClearingMoves(Board board) {
        Piece primary = board.getPrimaryPiece();
        if (primary == null) return Integer.MAX_VALUE;
        
        // 1. Calculate direct exit distance
        int directDistance = calculateDirectDistance(board);
        
        // 2. Identify critical path blockers
        Set<Piece> blockers = getCriticalBlockers(board);
        
        // 3. Calculate minimal clearing moves for each blocker
        int totalMoves = 0;
        for (Piece blocker : blockers) {
            int moves = calculateBlockerMoves(blocker, primary, board);
            if (moves == Integer.MAX_VALUE) return Integer.MAX_VALUE;
            totalMoves += moves;
        }
        
        return directDistance + totalMoves;
    }

    private Set<Piece> getCriticalBlockers(Board board) {
        Set<Piece> blockers = new HashSet<>();
        Piece primary = board.getPrimaryPiece();
        Exit exit = board.getExitSide();
        
        if (primary.getOrientation() == Orientation.HORIZONTAL) {
            int row = primary.getFirstPosition().row;
            int startCol = exit == Exit.RIGHT ? 
                primary.getRightmostCol() + 1 : 0;
            int endCol = exit == Exit.RIGHT ? 
                board.getWidth() : primary.getLeftmostCol();
            
            // Check primary's row and adjacent vertical blockers
            for (int col = startCol; col < endCol; col++) {
                // Direct blockers in path
                if (board.getGrid()[row][col] != '.' && 
                    board.getGrid()[row][col] != primary.getId()) {
                    blockers.add(board.getPieceAt(row, col));
                }
                
                // Vertical blockers crossing the path
                for (Piece p : board.getPieces()) {
                    if (p.getOrientation() == Orientation.VERTICAL && 
                        p.getLeftmostCol() == col && 
                        p.getTopmostRow() <= row && 
                        p.getBottommostRow() >= row) {
                        blockers.add(p);
                    }
                }
            }
        } else { // Vertical primary
            int col = primary.getFirstPosition().col;
            int startRow = exit == Exit.BOTTOM ? 
                primary.getBottommostRow() + 1 : 0;
            int endRow = exit == Exit.BOTTOM ? 
                board.getHeight() : primary.getTopmostRow();
            
            for (int row = startRow; row < endRow; row++) {
                // Direct blockers in path
                if (board.getGrid()[row][col] != '.' && 
                    board.getGrid()[row][col] != primary.getId()) {
                    blockers.add(board.getPieceAt(row, col));
                }
                
                // Horizontal blockers crossing the path
                for (Piece p : board.getPieces()) {
                    if (p.getOrientation() == Orientation.HORIZONTAL && 
                        p.getTopmostRow() == row && 
                        p.getLeftmostCol() <= col && 
                        p.getRightmostCol() >= col) {
                        blockers.add(p);
                    }
                }
            }
        }
        return blockers;
    }

    private int calculateBlockerMoves(Piece blocker, Piece primary, Board board) {
        // Determine available space in both possible directions
        int forwardSpace = calculateClearSpace(blocker, true, board);
        int backwardSpace = calculateClearSpace(blocker, false, board);
        
        // Minimum moves needed to clear the path
        int requiredClearance = getRequiredClearance(blocker, primary, board);
        
        int forwardMoves = (requiredClearance <= forwardSpace) ? 
            (blocker.getLength() + requiredClearance) : Integer.MAX_VALUE;
        int backwardMoves = (requiredClearance <= backwardSpace) ? 
            (blocker.getLength() + requiredClearance) : Integer.MAX_VALUE;
        
        return Math.min(forwardMoves, backwardMoves);
    }

    private int calculateClearSpace(Piece blocker, boolean moveForward, Board board) {
        int space = 0;
        if (blocker.getOrientation() == Orientation.HORIZONTAL) {
            int checkCol = moveForward ? 
                blocker.getRightmostCol() + 1 : blocker.getLeftmostCol() - 1;
            while (moveForward ? checkCol < board.getWidth() : checkCol >= 0) {
                boolean columnClear = true;
                for (int r = blocker.getTopmostRow(); r <= blocker.getBottommostRow(); r++) {
                    if (board.getGrid()[r][checkCol] != '.') {
                        columnClear = false;
                        break;
                    }
                }
                if (!columnClear) break;
                space++;
                checkCol += moveForward ? 1 : -1;
            }
        } else { // Vertical
            int checkRow = moveForward ? 
                blocker.getBottommostRow() + 1 : blocker.getTopmostRow() - 1;
            while (moveForward ? checkRow < board.getHeight() : checkRow >= 0) {
                boolean rowClear = true;
                for (int c = blocker.getLeftmostCol(); c <= blocker.getRightmostCol(); c++) {
                    if (board.getGrid()[checkRow][c] != '.') {
                        rowClear = false;
                        break;
                    }
                }
                if (!rowClear) break;
                space++;
                checkRow += moveForward ? 1 : -1;
            }
        }
        return space;
    }

    private int getRequiredClearance(Piece blocker, Piece primary, Board board) {
        // Calculate how far the blocker needs to move to clear the path
        if (primary.getOrientation() == Orientation.HORIZONTAL) {
            int blockerColSpan = blocker.getLeftmostCol() + blocker.getLength() - 1;
            return Math.max(0, blockerColSpan - primary.getRightmostCol() + 1);
        } else {
            int blockerRowSpan = blocker.getTopmostRow() + blocker.getLength() - 1;
            return Math.max(0, blockerRowSpan - primary.getBottommostRow() + 1);
        }
    }
        
    /**
     * Inner class representing a search node
     */
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