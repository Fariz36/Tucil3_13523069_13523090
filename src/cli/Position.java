package cli;

public class Position {
    public int row;
    public int col;
    
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }
    
    public Position(Position other) {
        this.row = other.row;
        this.col = other.col;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return row == position.row && col == position.col;
    }
    
    @Override
    public int hashCode() {
        return row * 100 + col;
    }
    
    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
