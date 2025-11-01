/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Mapa;
import Objetos.*;
import Objetos.Utilidad.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import mariotest.Mariotest;

/**
 * Gestor de Niveles - Patrón FACTORY + STRATEGY
 * Maneja carga, configuración y transiciones entre niveles
 * 
 * @author LENOVO
 */
public class GestorNiveles {
    
    private Mariotest juego;
    private Handler handler;
    private EstadoNivel estadoActual;
    
    // Configuración de niveles
    private int nivelActual;
    private static final int NIVEL_INICIAL = 1;
    private static final int NIVEL_MAXIMO = 4;
    
    // Configuración específica por nivel
    private ConfiguracionNivel configActual;
    
    // Referencias a sistemas
    private BarrilSpawner barrelSpawner;
    private FuegoSpawner fuegoSpawner;
    private ItemSpawner itemSpawner;
    private TiledTMXParser tmxParser;
    
    // Sprites de animación de victoria
    private BufferedImage spriteCorazon;
    private BufferedImage spriteCorazonRoto;
    private BufferedImage[] spritesDKAgarra;
    
    // Referencias a entidades clave
    private DiegoKong diegoKong;
    private Princesa princesa;
    
    // Estado de animación de victoria
    private boolean animacionVictoriaActiva;
    private int frameAnimacionDK;
    
    /**
     * Constructor
     */
    public GestorNiveles(Mariotest juego, Handler handler) {
        this.juego = juego;
        this.handler = handler;
        this.nivelActual = NIVEL_INICIAL;
        this.estadoActual = new EstadoNivel.Jugando(juego, this);
        this.tmxParser = new TiledTMXParser(handler);
        this.animacionVictoriaActiva = false;
        
        cargarSpritesVictoria();
    }
    
    /**
     * Carga sprites de animación de victoria
     */
    private void cargarSpritesVictoria() {
        try {
            // TODO: Cargar sprites desde Texturas
            spriteCorazon = Mariotest.getTextura().getCorazonSprite();
            spriteCorazonRoto = Mariotest.getTextura().getCorazonRotoSprite();
            spritesDKAgarra = Mariotest.getTextura().getDKAgarraSprites();
            
            System.out.println("[GESTOR] Sprites de victoria cargados");
        } catch (Exception e) {
            System.err.println("[ERROR] Fallo al cargar sprites de victoria: " + e.getMessage());
        }
    }
    
    /**
     * Inicializa el nivel actual
     */
    public void inicializarNivel(int nivel) {
        System.out.println("\n========================================");
        System.out.println("  INICIANDO NIVEL " + nivel);
        System.out.println("========================================");
        
        this.nivelActual = nivel;
        
        // Limpiar nivel anterior
        limpiarNivel();
        
        // Cargar configuración del nivel
        configActual = ConfiguracionNivel.crear(nivel);
        
        // Cargar mapa TMX
        cargarMapa(configActual.getRutaTMX());
        
        // Configurar elementos específicos del nivel
        configurarNivel(configActual);
        
        // Cambiar a estado JUGANDO
        cambiarEstado(new EstadoNivel.Jugando(juego, this));
        
        System.out.println("========================================");
        System.out.println("  NIVEL " + nivel + " CARGADO");
        System.out.println("========================================\n");
    }
    
    /**
     * Limpia todos los objetos del nivel anterior
     */
    private void limpiarNivel() {
        System.out.println("[GESTOR] Limpiando nivel anterior...");
        
        // Detener spawners
        if (barrelSpawner != null) barrelSpawner.desactivar();
        if (fuegoSpawner != null) fuegoSpawner.desactivar();
        if (itemSpawner != null) itemSpawner.desactivar();
        
        // Limpiar todos los objetos excepto el jugador
        Player player = handler.getPlayer();
        handler.getGameObjs().clear();
        
        // Re-agregar jugador
        if (player != null) {
            handler.addObj(player);
        }
        
        // Resetear referencias
        diegoKong = null;
        princesa = null;
    }
    
    /**
     * Carga el mapa TMX del nivel
     */
    private void cargarMapa(String rutaTMX) {
        System.out.println("[GESTOR] Cargando mapa: " + rutaTMX);
        tmxParser.cargarMapa(rutaTMX);
    }
    
    /**
     * Configura elementos específicos del nivel
     */
    private void configurarNivel(ConfiguracionNivel config) {
        // Crear Diego Kong
        if (config.tieneDiegoKong()) {
            Point posDK = config.getPosicionDK();
            diegoKong = new DiegoKong(posDK.x, posDK.y, 2, handler);
            handler.addObj(diegoKong);
        }
        
        // Crear Princesa
        if (config.tienePrincesa()) {
            Point posPrincesa = config.getPosicionPrincesa();
            princesa = new Princesa(posPrincesa.x, posPrincesa.y, 2, handler);
            handler.addObj(princesa);
        }
        
        // Configurar spawner de barriles
        if (config.tieneBarriles()) {
            List<Point> spawnPoints = config.getBarrilSpawnPoints();
            barrelSpawner = new BarrilSpawner(handler, spawnPoints);
            if (config.isBarrilesActivos()) {
                barrelSpawner.activar();
            }
        }
        
        // Configurar spawner de fuegos
        if (config.tieneFuegos()) {
            List<Point> spawnPoints = config.getFuegoSpawnPoints();
            fuegoSpawner = new FuegoSpawner(handler, spawnPoints);
            fuegoSpawner.setMaxFuegos(config.getMaxFuegos());
            if (config.isFuegosActivos()) {
                fuegoSpawner.activar();
            }
        }
        
        // Configurar spawner de items
        if (config.tieneItems()) {
            List<Point> spawnPoints = config.getItemSpawnPoints();
            itemSpawner = new ItemSpawner(handler, spawnPoints);
            if (config.isItemsActivos()) {
                itemSpawner.activar();
            }
        }
        
        // Crear plataformas móviles específicas del nivel
        if (config.tienePlataformasMoviles()) {
            crearPlataformasMoviles(config);
        }
        
        // Crear llamas estáticas
        if (config.tieneLlamasEstaticas()) {
            crearLlamasEstaticas(config);
        }
    }
    
    /**
     * Crea plataformas móviles según configuración
     */
    private void crearPlataformasMoviles(ConfiguracionNivel config) {
        for (PlataformaConfig pConfig : config.getPlataformasMoviles()) {
            PlataformaMovil plataforma = new PlataformaMovil(
                pConfig.x, pConfig.y,
                pConfig.width, pConfig.height,
                pConfig.scale, pConfig.tileID,
                pConfig.tipo, pConfig.velocidad,
                pConfig.limiteMin, pConfig.limiteMax,
                pConfig.duracionVisible, pConfig.duracionInvisible
            );
            handler.addObj(plataforma);
        }
    }
    
    /**
     * Crea llamas estáticas según configuración
     */
    private void crearLlamasEstaticas(ConfiguracionNivel config) {
        for (Point pos : config.getPosicionesLlamasEstaticas()) {
            LlamaEstatica llama = new LlamaEstatica(pos.x, pos.y, 2, handler);
            handler.addObj(llama);
        }
    }
    
    /**
     * Verifica si el jugador llegó a la princesa (victoria)
     */
    public boolean verificarVictoria() {
        if (princesa == null || princesa.isRescatada()) {
            return false;
        }
        
        Player player = handler.getPlayer();
        if (player == null) return false;
        
        // Verificar distancia
        float distX = Math.abs(player.getX() - princesa.getX());
        float distY = Math.abs(player.getY() - princesa.getY());
        
        if (distX < 30 && distY < 30) {
            princesa.setRescatada(true);
            return true;
        }
        
        return false;
    }
    
    /**
     * Inicia la animación de victoria
     */
    public void iniciarAnimacionVictoria() {
        animacionVictoriaActiva = true;
        frameAnimacionDK = 0;
        
        System.out.println("[VICTORIA] Iniciando animación de victoria");
    }
    
    /**
     * Muestra sprite de corazón
     */
    public void mostrarCorazon() {
        System.out.println("[VICTORIA] 💖 Mostrando corazón");
        // El sprite se renderiza en renderOverlayVictoria()
    }
    
    /**
     * Anima a DK agarrando a la princesa
     */
    public void animarDKAgarraPrincesa() {
        System.out.println("[VICTORIA] 🦍 DK agarra a la princesa");
        
        if (diegoKong != null && princesa != null) {
            // Animar DK (cambiar a animación especial)
            // diegoKong.activarAnimacionAgarrar();
            
            // Mover princesa hacia DK
            // princesa.moverHacia(diegoKong.getX(), diegoKong.getY());
        }
    }
    
    /**
     * Muestra sprite de corazón roto
     */
    public void mostrarCorazonRoto() {
        System.out.println("[VICTORIA] 💔 Mostrando corazón roto");
        // El sprite se renderiza en renderOverlayVictoria()
    }
    
    /**
     * Renderiza overlay de victoria
     */
    public void renderOverlayVictoria(Graphics g, int fase, int ticks) {
        if (!animacionVictoriaActiva) return;
        
        // Overlay semi-transparente
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, Mariotest.getVentanaWidth(), Mariotest.getVentanaHeight());
        
        int centerX = Mariotest.getVentanaWidth() / 2;
        int centerY = Mariotest.getVentanaHeight() / 2;
        
        switch (fase) {
            case 0: // Corazón
                if (spriteCorazon != null) {
                    g.drawImage(spriteCorazon, centerX - 32, centerY - 32, 64, 64, null);
                } else {
                    // Placeholder
                    g.setColor(Color.RED);
                    g.fillOval(centerX - 30, centerY - 30, 60, 60);
                }
                break;
                
            case 1: // DK agarra princesa
                // Renderizar animación de DK
                g.setColor(Color.WHITE);
                g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
                g.drawString("DK AGARRA A LA PRINCESA", centerX - 150, centerY);
                break;
                
            case 2: // Corazón roto
                if (spriteCorazonRoto != null) {
                    g.drawImage(spriteCorazonRoto, centerX - 32, centerY - 32, 64, 64, null);
                } else {
                    // Placeholder
                    g.setColor(new Color(139, 0, 0));
                    g.fillOval(centerX - 30, centerY - 30, 60, 60);
                    g.setColor(Color.BLACK);
                    g.drawLine(centerX - 30, centerY - 30, centerX + 30, centerY + 30);
                    g.drawLine(centerX + 30, centerY - 30, centerX - 30, centerY + 30);
                }
                break;
        }
    }
    
    /**
     * Carga el siguiente nivel
     */
    public void cargarSiguienteNivel() {
        int siguienteNivel = nivelActual + 1;
        
        if (siguienteNivel > NIVEL_MAXIMO) {
            System.out.println("[GESTOR] ¡Juego completado!");
            // TODO: Mostrar pantalla de victoria final
            siguienteNivel = NIVEL_INICIAL; // Reiniciar al nivel 1
        }
        
        inicializarNivel(siguienteNivel);
    }
    
    /**
     * Detiene todos los spawners
     */
    public void detenerSpawners() {
        if (barrelSpawner != null) barrelSpawner.desactivar();
        if (fuegoSpawner != null) fuegoSpawner.desactivar();
        if (itemSpawner != null) itemSpawner.desactivar();
    }
    
    /**
     * Cambia el estado del nivel
     */
    public void cambiarEstado(EstadoNivel nuevoEstado) {
        if (estadoActual != null) {
            estadoActual.salir();
        }
        
        estadoActual = nuevoEstado;
        estadoActual.entrar();
    }
    
    /**
     * Actualiza el gestor de niveles
     */
    public void tick() {
        if (estadoActual != null) {
            estadoActual.tick();
        }
        
        // Actualizar spawners solo si el estado lo permite
        if (estadoActual.permitirSpawnEnemigos()) {
            if (barrelSpawner != null) barrelSpawner.tick();
            if (fuegoSpawner != null) fuegoSpawner.tick();
            if (itemSpawner != null) itemSpawner.tick();
        }
    }
    
    /**
     * Renderiza el overlay del estado actual
     */
    public void render(Graphics g) {
        if (estadoActual != null) {
            estadoActual.render(g);
        }
    }
    
    // ==================== GETTERS ====================
    
    public int getNivelActual() {
        return nivelActual;
    }
    
    public EstadoNivel getEstadoActual() {
        return estadoActual;
    }
    
    public boolean permitirMovimientoJugador() {
        return estadoActual != null && estadoActual.permitirMovimientoJugador();
    }
    
    public BarrilSpawner getBarrelSpawner() {
        return barrelSpawner;
    }
    
    public FuegoSpawner getFuegoSpawner() {
        return fuegoSpawner;
    }
    
    public ItemSpawner getItemSpawner() {
        return itemSpawner;
    }
}