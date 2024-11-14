package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.Repository.SerieRepository;
import com.aluracursos.screenmatch.model.*;
import com.aluracursos.screenmatch.service.ConsumoAPI;
import com.aluracursos.screenmatch.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=33aca484";
    private ConvierteDatos conversor = new ConvierteDatos();
    private List<DatosSerie> datosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series;
    Optional<Serie> serieBuscada;

    public Principal(SerieRepository repository) {
        this.repositorio = repository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1 - Buscar series 
                    2 - Buscar episodios
                    3 - Mostrar series buscadas
                    4 - Buscar serie por nombre
                    5 - Top 5 series hoy
                    6 - Busqueda por genero
                    7 - Busqueda temporada y evaluacion
                    8 - Buscar episodios por titulo
                    9 - Buscar top 5 episodios hoy
                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodio();
                    break;
                case 3:
                    mostrarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarTop5SeriesHoy();
                    break;
                case 6:
                    busquedaPorGenero();
                    break;
                case 7:
                    busquedaTemporadaEvaluacion();
                    break;
                case 8:
                    busquedaEpisodioPorTitulo();
                    break;
                case 9:
                    buscarTop5EpisodiosHoy();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }

    }

    private void buscarSeriePorTitulo() {
        System.out.println("Que deseas ver hoy?");
        var nombreSerie = teclado.nextLine();
        serieBuscada = repositorio.findByTituloContainingIgnoreCase(nombreSerie);

        if (serieBuscada.isPresent()) {
            System.out.println("Quisiste decir: " + serieBuscada.get());
        } else {
            System.out.println("Serie no encontrada");
        }
    }

    private DatosSerie getDatosSerie() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + nombreSerie.replace(" ", "+") + API_KEY);
        System.out.println(json);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        return datos;
    }

    private void buscarEpisodio() {
        System.out.println("Resultados");
        mostrarSeriesBuscadas();
        System.out.println("Que serie desea consultar");
        var nombreSerie = teclado.nextLine();

        Optional<Serie> serie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nombreSerie.toLowerCase()))
                .findFirst();
        if (serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DatosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumoApi.obtenerDatos(URL_BASE + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                System.out.println(json);
                DatosTemporadas datosTemporada = conversor.obtenerDatos(json, DatosTemporadas.class);
                temporadas.add(datosTemporada);
            }
            temporadas.forEach(System.out::println);
            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> {
                                Episodio episodio = new Episodio(d.numero(), e);
                                System.out.println("Mapped Episode: " + episodio);
                                return episodio;
                            }))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
            System.out.println(episodios);
            System.out.println(temporadas);

        }
//
    }

    private void buscarSerieWeb() {
        DatosSerie datos = getDatosSerie();
        Serie serie = new Serie(datos);
        repositorio.save(serie);
        //datosSeries.add(datos);
        System.out.println(datos);
    }

    private void mostrarSeriesBuscadas() {
        series = repositorio.findAll();


        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarTop5SeriesHoy() {
        List<Serie> topSerie = repositorio.findTop5ByOrderByEvaluacionDesc();
        topSerie.forEach(s ->
                System.out.println("Serie: " + s.getTitulo() + "Evaluacion: " + s.getEvaluacion()));

    }

    private void busquedaPorGenero() {
        System.out.println("Que genero desea ver?");
        var genero = teclado.nextLine();
        var categoria = Categoria.fromEspanol(genero);
        List<Serie> seriePorGenero = repositorio.findByGenero(categoria);
        System.out.println("Resultados de " + genero + ":");
        seriePorGenero.forEach(System.out::println);
    }

    private void busquedaTemporadaEvaluacion() {
        System.out.println("Cuantas temporadas");
        var temporada = teclado.nextInt();

        System.out.println("Que evaluacion");
        var evalua = teclado.nextDouble();

        //busqueda temporada y evaluacion

        // List<Serie> busquedaTemporadaEvaluacion = repositorio.findByTotalTemporadasLessThanEqualAndEvaluacionGreaterThanEqual(temporada, evalua);
        // Temporada y evaluacion  native queries

       /* List<Serie> busquedaTemporadaEvaluacion = repositorio.seriesPorTemporadaYEvaluacion();
        busquedaTemporadaEvaluacion.forEach(s ->
                System.out.println(s.getTitulo() + "Evaluacion " + s.getEvaluacion()));*/

        List<Serie> busquedaTemporada = repositorio.seriesPorTemporadaYEvaluacion(temporada, evalua);
        busquedaTemporada.forEach(s ->
                System.out.println(s.getTitulo() + "Evaluacion " + s.getEvaluacion()));
    }

    private void  busquedaEpisodioPorTitulo(){
        System.out.println("Escribe el nombre del episodio que deseas buscar");
        var nombreEpisodio = teclado.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorNombre(nombreEpisodio);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Serie: %s Temporada %s Episodio %s Evaluación %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getEvaluacion()));

    }
    private void buscarTop5EpisodiosHoy(){
        buscarSeriePorTitulo();
        if (serieBuscada.isPresent()){
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodios = repositorio.top5episodios(serie);
            topEpisodios.forEach(e ->
                    System.out.printf("Serie: %s Temporada %s Titulo %s Evaluación %s\n",
                            e.getSerie().getTitulo(), e.getTemporada(), e.getTitulo(), e.getEvaluacion()));
        }
    }

}




