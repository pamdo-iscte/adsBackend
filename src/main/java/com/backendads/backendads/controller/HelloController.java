package com.backendads.backendads.controller;

import Files.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.nio.file.Files;


@RestController
public class HelloController {
    private final String primeiro_dia_de_aulas = "2022/09/12";
    private FuncoesAuxiliares aux = new FuncoesAuxiliares();
    private Calendar primeiro_dia_de_aulas_cal = aux.setCalendar(Calendar.getInstance(),primeiro_dia_de_aulas.split("/"));

    private List<String> colors = Arrays.asList("#1cceb1", "#97fca3", "#5d8ce9","#6cda72","#a1f2e5","#9799fc","#fcf897");
    private int index_of_colors = 0;

    private final String dir_horariosCriados="HorariosCriados";
    private final String dir_horariosCompletos="HorariosCompletos";
    private Main main;

    @GetMapping("/get_metodos")
    public String get_metodos() {
        main = new Main();
        List<List<String>> metodos = new ArrayList<>();
        List<String> nomes_metodos_aulas = new ArrayList<>();
        List<String> nomes_metodos_avaliacoes = new ArrayList<>();

        Class<MetodosParaAulas> aulas = MetodosParaAulas.class;
        Class<MetodosdeAvaliacao> avaliacoes = MetodosdeAvaliacao.class;
        Method[] methods_aulas = aulas.getDeclaredMethods();
        Method[] methods_avaliacoes = avaliacoes.getDeclaredMethods();

        for (Method metodo_aula : methods_aulas) nomes_metodos_aulas.add(aux.replace_nome_metodo(metodo_aula.getName()));
        for (Method metodo_avaliacao : methods_avaliacoes) nomes_metodos_avaliacoes.add(aux.replace_nome_metodo(metodo_avaliacao.getName()));

        metodos.add(nomes_metodos_aulas);metodos.add(nomes_metodos_avaliacoes);
        return new Gson().toJson(metodos);
    }

    @GetMapping("/get_aluno_professor")
    public String aulas() throws IOException {
        index_of_colors = 0;
        String str_file = Files.readString(Path.of("horario_sem_aulas_repetidas.json"));
        if (str_file.equals("")) {
            List<Convert_Aula_CSV_to_JSON> lista_de_aulas_com_aulas_unicas = aux.get_Dias_da_semana(aux.getAulas());
            System.out.println("Size: "+lista_de_aulas_com_aulas_unicas.size());
            String str_list = new Gson().toJson(lista_de_aulas_com_aulas_unicas);
            try {
                FileWriter file = new FileWriter("horario_sem_aulas_repetidas.json");
                file.write(str_list);
                file.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return str_list;
        }
        else return str_file;
    }

    @PostMapping("/obter_metodos_selecionados")
    public String obter_metodos_selecionados(@RequestBody MetodosSelecionados json_metodos) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        List<String> aulas = json_metodos.getAulas();
        System.out.println("Aulas antes do replace: "+aulas);
        List<String> avaliacoes = json_metodos.getAvaliacoes();
        System.out.println("Avaliacoes antes do replace: "+avaliacoes);
        aulas.replaceAll(s -> (s.substring(0, 1).toLowerCase() + s.substring(1)).replace(" ", "_"));
        avaliacoes.replaceAll(s -> (s.substring(0, 1).toLowerCase() + s.substring(1)).replace(" ", "_"));

        System.out.println("Metodos Aulas: "+aulas);
        System.out.println("Metodos Aulas: "+avaliacoes);
        main.start(aulas,avaliacoes);
        //objetivo é dar return ao filename do horario criado
        return "";
    }

    @PostMapping("/obter_aulas_da_UC_escolhida")
    public String obter_aulas_da_UC_escolhida(@RequestBody UC_escolhida uc) {
        List<Slot_horario_semestral> slots = new ArrayList<>();

        String[] horarios_das_aulas = aux.split_list_elements(uc.getHoras());
        String[] dias_de_semana = aux.split_list_elements(uc.getDias());
        List<String> datas = uc.getDatas();
        List<String> horas_repetidas = uc.getHoras_repetidas();

        String color= aux.setColor_evento(colors,index_of_colors);
        index_of_colors++;
        String sigla = aux.obter_sigla_da_uc(uc.getUnidade_de_execucao());

        for (int i = 0; i < horarios_das_aulas.length; i++) {
            String dia_de_sem = dias_de_semana[i];
            if (i>0) {dia_de_sem = aux.substring_a_str(dia_de_sem);horarios_das_aulas[i]=aux.substring_a_str(horarios_das_aulas[i]);}
            String[] hora_inicio_fim = horarios_das_aulas[i].split(";");
//            System.out.println(hora_inicio_fim[0] + " "+ hora_inicio_fim[1]);
            String id = uc.getTurno() + dia_de_sem + hora_inicio_fim[0]+hora_inicio_fim[1];
            String informacao_detalhada = uc.getUnidade_de_execucao() +" | Curso(s): "+uc.getCurso()+" | Semanas: ";
//            System.out.println("\nDia de sem: "+dia_de_sem+" Horas: "+horarios_das_aulas[i]);
            List<Integer> number_of_weeks = aux.get_number_of_weeks_of_slot(datas, horas_repetidas, primeiro_dia_de_aulas_cal,
                    dia_de_sem, horarios_das_aulas[i]);
            Collections.sort(number_of_weeks);
//            System.out.println(aux.reduzir_list_number_of_weeks(number_of_weeks));
            String number_of_weeks_reduzida = aux.reduzir_list_number_of_weeks(number_of_weeks);
            informacao_detalhada = informacao_detalhada.concat(number_of_weeks_reduzida);
            String text = sigla + " | " + number_of_weeks_reduzida;

            //"2022-12-06T10:30:00"
            String data_ajustada = aux.ajustar_data_horario_sem(dia_de_sem);
            String start = data_ajustada + "T" + hora_inicio_fim[0];
            String end = data_ajustada + "T" + hora_inicio_fim[1];

            slots.add(new Slot_horario_semestral(id, text, start, end, color,informacao_detalhada,uc.getTurno()));
        }
//        System.out.println(new Gson().toJson(slots));
        return new Gson().toJson(slots);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) {

        String fileName = file.getOriginalFilename();
        System.out.println(fileName);
        String send = new Gson().toJson("File uploaded successfully.");
        return ResponseEntity.ok(send);
    }


    @PostMapping("/guardar_horario")
    public String guardar_horario(@RequestBody ReceiveClasses slots)  {
        //enviar as duas listas Convert_Aula_CSV_to_JSON e Slot_horario_Semestral
        try {
            FileOutputStream fo = new FileOutputStream(dir_horariosCriados + "\\" + slots.getNum() + ".txt");
            ObjectOutputStream oo = new ObjectOutputStream(fo);

            for (Slot_horario_semestral slot : slots.getSlots()) {
                System.out.println(slot.toString());
                oo.writeObject(slot);
            }
            oo.writeObject(null);

            oo.close();
            fo.close();

            aux.guardar_horario_completo(slots.getAulas(), Integer.parseInt(slots.getNum()),dir_horariosCompletos);
            return "Horário guardado";
        } catch (IOException e) {
            String mensagem_de_erro = "Ocorreu um erro ao guardar o horário do aluno/docente número "+slots.getNum();
            System.err.println(mensagem_de_erro);
            return mensagem_de_erro;
        }
    }

    @PostMapping("/ler_horario_semestral_guardado")
    public String ler_horario_semestral_guardado(@RequestBody JsonNode json) {
        List<Slot_horario_semestral> slots = aux.read_file(dir_horariosCriados,json.get("num").asText());

        for (Slot_horario_semestral s: slots) {
            System.out.println(s.toString());
        }
        return "OLA";
    }
    @PostMapping("/fileexists")
    public String FileExists(@RequestBody NameOfFile file) throws IOException {
        System.out.println(dir_horariosCriados + "\\"+file.getFile()+".txt");
        File tempFile = new File(dir_horariosCriados + "\\"+file.getFile()+".txt");
        boolean exists = tempFile.exists();
        System.out.println(exists);//true
        return new Gson().toJson(exists);
    }

    @PostMapping("/obter_horario_de_uma_semana")
    public String obter_horario_de_uma_semana(@RequestBody JsonNode json) {
        String data = json.get("data").asText().split("T")[0];
        String num = json.get("num").asText();

        System.out.println(data + " "+num);
        Calendar calendar = Calendar.getInstance();
        calendar = aux.setCalendar(calendar,data.split("-"));

        List<Slot_horario_semestral> slots = aux.read_file(dir_horariosCompletos,num);
        System.out.println("        Size: "+slots.size());
        List<Slot_horario_semestral> horario_da_semana = new ArrayList<>();

        List<String> turnos_UCs = new ArrayList<>();

        for (Slot_horario_semestral slot :slots) {
            System.out.println(slot.getCalendar().getTime());
            System.out.println(calendar.getTime());
            if (calendar.get(Calendar.WEEK_OF_YEAR) == slot.getCalendar().get(Calendar.WEEK_OF_YEAR)) {
                horario_da_semana.add(slot);
                if (!turnos_UCs.contains(slot.getTurno())) turnos_UCs.add(slot.getTurno());
            }
        }

        for (Slot_horario_semestral aula: horario_da_semana) {
            for (int i=0; i<turnos_UCs.size();i++) {
                String color ="";
                if (i == colors.size()) {
                    Random random = new Random();
                    int nextInt = random.nextInt(0xffffff + 1);
                    color = String.format("#%06x", nextInt);
                }
                else if (turnos_UCs.get(i).equals(aula.getTurno())) color = colors.get(i);
                aula.setBackColor(color);
            }
        }

        return new Gson().toJson(horario_da_semana);
    }


    @PostMapping("/reformular_horario")
    public List<String> reformular_horario(@RequestBody JsonNode json) {
        String num = json.get("num").asText();

        List<Slot_horario_semestral> slots = aux.read_file(dir_horariosCriados,num);

        List<String> turnos = new ArrayList<>();
        if (slots.isEmpty()) return turnos;

        for (Slot_horario_semestral slot: slots) {
            if (!turnos.contains(slot.getTurno())) turnos.add(slot.getTurno());
        }

        return turnos;
    }


}