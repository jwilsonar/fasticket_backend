package pe.edu.pucp.fasticket.model.eventos;

public enum Etapa {
    PREVENTA(0.75),
    EARLY_BIRD(0.90),
    REGULAR(1.0),
    LATE(1.10);

    private final double multiplicador;

    Etapa(double multiplicador) {
        this.multiplicador = multiplicador;
    }

    public double getMultiplicador() {
        return multiplicador;
    }
}
