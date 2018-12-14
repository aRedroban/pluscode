/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package paq_reportes_solidario;

/**
 *
 * @author ANDRES
 */
import static com.lowagie.text.pdf.PdfFileSpecification.url;
import framework.aplicacion.TablaGenerica;
import framework.componentes.Boton;
import framework.componentes.Calendario;
import sistema.aplicacion.Pantalla;
import framework.componentes.Division;
import framework.componentes.Etiqueta;
import framework.componentes.PanelTabla;
import framework.componentes.Reporte;
import framework.componentes.SeleccionFormatoReporte;
import framework.componentes.Tabla;
import framework.componentes.VisualizarPDF;
import framework.reportes.GenerarReporte;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.util.JRLoader;

import paq_solidario.ejb.ServicioSolidario;
import persistencia.Conexion;

public class pre_solidario_reporte extends Pantalla{
    private Tabla tab_tabla = new Tabla();
    private Calendario fechaInicio = new Calendario();
    private Calendario fechaFin = new Calendario();
    private VisualizarPDF vipdf_comprobante = new VisualizarPDF();
    private VisualizarPDF vipdf_contrato = new VisualizarPDF();
    private Reporte rep_reporte = new Reporte(); //Listado de Reportes, siempre se llama rep_reporte
    private SeleccionFormatoReporte sel_rep = new SeleccionFormatoReporte(); //formato de salida del reporte
    private Map map_parametros = new HashMap();//Parametros del reporte
    String cliente = "";
    double total_ingresos = 0;
    double total_gastos = 0;
    double total_gastos_ingresos = 0;
    double patrimonio = 0;
    //double total_gastos_ingresos = 0;
    @EJB
    private final ServicioSolidario ser_solidario = (ServicioSolidario) utilitario.instanciarEJB(ServicioSolidario.class);
    public pre_solidario_reporte(){
         tab_tabla.setId("tab_tabla");
         tab_tabla.setSql(ser_solidario.getClientesExitosos("2", "", "")); //+" and c.id = -1"
         tab_tabla.setRows(500);
         tab_tabla.setLectura(true);
         tab_tabla.dibujar();
         
         PanelTabla pat_panel = new PanelTabla();
         pat_panel.setPanelTabla(tab_tabla);

         Division div_division = new Division();
         div_division.setId("div_division");
         div_division.dividir1(pat_panel);
         
         agregarComponente(div_division);
         
         bar_botones.agregarComponente(new Etiqueta("FECHA DESDE :"));
         fechaInicio.setId("fechaInicio");
         fechaInicio.setFechaActual();
         fechaInicio.setTipoBoton(true);
         bar_botones.agregarComponente(fechaInicio);
         bar_botones.agregarComponente(new Etiqueta("FECHA HASTA :"));
         fechaFin.setId("fechaFin");
         fechaFin.setFechaActual();
         fechaFin.setTipoBoton(true);
         bar_botones.agregarComponente(fechaFin);
          Boton bot_consultar = new Boton();
          bot_consultar.setTitle("BUSCAR");
          bot_consultar.setMetodo("actualizarFechas");
          bot_consultar.setIcon("ui-icon-search");
          bar_botones.agregarComponente(bot_consultar);
          bar_botones.agregarSeparador();
          Boton bot_clean = new Boton();
          bot_clean.setIcon("ui-icon-print");
          bot_clean.setValue("IMPRIMIR SOLICITUD");
          bot_clean.setMetodo("generarReporte");
          bar_botones.agregarBoton(bot_clean);
          
          Boton bot_contrato = new Boton();
          bot_contrato.setIcon("ui-icon-print");
          bot_contrato.setValue("IMPRIMIR CONTRATO");
          bot_contrato.setMetodo("generarContrato");
          bar_botones.agregarBoton(bot_contrato);
          
          Boton bot_prueba = new Boton();
          bot_prueba.setIcon("ui-icon-print");
          bot_prueba.setValue("PROBAR");
          bot_prueba.setMetodo("imprimirReportes");
          bar_botones.agregarBoton(bot_prueba);
          
          sel_rep.setId("sel_rep");
          agregarComponente(sel_rep);
          vipdf_comprobante.setId("vipdf_comprobante");
          vipdf_comprobante.setTitle("SOLICITUD DE TARJETA SOLIDARIO");
          agregarComponente(vipdf_comprobante);
          
          vipdf_contrato.setId("vipdf_contrato");
          vipdf_contrato.setTitle("SOLICITUD DE CONTRATO SOLIDARIO");
          agregarComponente(vipdf_contrato);
    }
    public void actualizarFechas(){
        String fecha1 = utilitario.getFormatoFecha(fechaInicio.getValue());
        String fecha2 = utilitario.getFormatoFecha(fechaFin.getValue());
        
    try{
            tab_tabla.setSql(ser_solidario.getClientesExitosos("3", fecha1, fecha2));
            tab_tabla.ejecutarSql();
            utilitario.addUpdate("tab_tabla");
          //  System.out.println("fecha1: "+fecha1);
         //   System.out.println("fecha2: "+fecha2);
         if (tab_tabla.isEmpty()){
             utilitario.agregarMensajeInfo("No existen clientes exitosos en las fechas establecidas", "Verifique las fechas");
         }
    } catch (Exception e){
        
    }
        
    }
    public void calculaValores(){
        double sueldo_ingresos = 0;
        double rentas = 0;
        double remesas = 0;
        double ingreso_familiares = 0;
        double otros_ingresos = 0;
        double gastos_basicos = 0;
        double servicios_basicos = 0;
        double transporte = 0;
        double otros_gastos = 0;
        double valor_unitario_bien = 0;
        double saldo_pasivo = 0;
        TablaGenerica tab_ingresos = utilitario.consultar("select sueldo_ingresos, rentas, remesas, ingreso_familiares, otros_ingresos from clientes where id ="+cliente);
        TablaGenerica tab_gastos = utilitario.consultar("select gastos_basicos, servicios_basicos, transporte, otros_gastos from clientes where id = "+cliente);
        TablaGenerica tab_patrimonio = utilitario.consultar("select valor_unitario_bien, saldo_pasivo from clientes where id ="+cliente);
        try{
            sueldo_ingresos = Double.parseDouble(tab_ingresos.getValor("sueldo_ingresos"));
            rentas = Double.parseDouble(tab_ingresos.getValor("rentas"));
            remesas = Double.parseDouble(tab_ingresos.getValor("remesas"));
            ingreso_familiares = Double.parseDouble(tab_ingresos.getValor("ingreso_familiares"));
            otros_ingresos = Double.parseDouble(tab_ingresos.getValor("otros_ingresos"));
            total_ingresos = sueldo_ingresos + rentas + remesas + ingreso_familiares + otros_ingresos;
            
            gastos_basicos = Double.parseDouble(tab_gastos.getValor("gastos_basicos"));
            servicios_basicos = Double.parseDouble(tab_gastos.getValor("servicios_basicos"));
            transporte = Double.parseDouble(tab_gastos.getValor("transporte"));
            otros_gastos = Double.parseDouble(tab_gastos.getValor("otros_gastos"));
            total_gastos = gastos_basicos + servicios_basicos + transporte + otros_gastos;
            
            total_gastos_ingresos = total_ingresos - total_gastos;
            
            valor_unitario_bien = Double.parseDouble(tab_patrimonio.getValor("valor_unitario_bien"));
            saldo_pasivo = Double.parseDouble(tab_patrimonio.getValor("saldo_pasivo"));
            patrimonio = valor_unitario_bien - saldo_pasivo;
        } catch (Exception e){
            
        }
        //System.out.println("sueldo: "+sueldo_ingresos);
        //System.out.println("rentas: "+rentas);
        //System.out.println("remesas: "+remesas);
       // System.out.println("total_ingresos: "+total_ingresos);
       // System.out.println("total_gastos: "+total_gastos);
       // System.out.println("total_gastos_ingresos: "+utilitario.getFormatoNumero(total_gastos_ingresos));
    }
    public void generarReporte(){
        
        cliente = tab_tabla.getValorSeleccionado();
        calculaValores();
        if (tab_tabla.getValorSeleccionado() != null){
             Map parametros = new HashMap();
             parametros.put("pide_cliente", tab_tabla.getValorSeleccionado());
             parametros.put("total_ingresos", utilitario.getFormatoNumero(total_ingresos));
             parametros.put("total_gastos", utilitario.getFormatoNumero(total_gastos));
             parametros.put("total_ingresos_gastos", utilitario.getFormatoNumero(total_gastos_ingresos));
             parametros.put("patrimonio", utilitario.getFormatoNumero(patrimonio));
             vipdf_comprobante.setVisualizarPDF("rep_solidario/rep_solicitud_solidario.jasper", parametros);
             //sub_sub_rep_solidario.jasper
             //vipdf_comprobante.setVisualizarPDF("rep_solidario/sub_sub_rep_solidario.jasper", parametros);
             vipdf_comprobante.dibujar();
             utilitario.addUpdate("vipdf_comprobante");
             //utilitario.getConexion().ejecutarSql("UPDATE clientes SET es_impreso = 1 where id ="+cliente);
             //tab_tabla.actualizar();
              //utilitario.addUpdate("tab_tabla");
        }else {
            utilitario.agregarMensajeInfo("Seleccione un cliente para imprimir la solicitud de tarjeta", "");
        }
      //  utilitario.agregarMensaje("fecha: "+ cal_fecha_inicio.getValue(), "");
    }
    public void generarContrato(){
        
        cliente = tab_tabla.getValorSeleccionado();
        if (tab_tabla.getValorSeleccionado() != null){
             Map parametros = new HashMap();
             parametros.put("pide_cliente", tab_tabla.getValorSeleccionado());
             vipdf_contrato.setVisualizarPDF("rep_solidario/sub_report_3.jasper", parametros);
             vipdf_contrato.dibujar();
             utilitario.addUpdate("vipdf_contrato");
             //utilitario.getConexion().ejecutarSql("UPDATE clientes SET es_impreso = 1 where id ="+cliente);
              //tab_tabla.actualizar();
              //utilitario.addUpdate("tab_tabla");
        }else {
            utilitario.agregarMensajeInfo("Seleccione un cliente para imprimir la solicitud de tarjeta", "");
        }
      //  utilitario.agregarMensaje("fecha: "+ cal_fecha_inicio.getValue(), "");
    }
    public void imprimirReportes(){
        Conexion conn = new Conexion();
      //  conn.NOMBRE_MARCA_BASE.equals("mysql");
        Map parametros = new HashMap();
        parametros.put("pide_cliente", tab_tabla.getValorSeleccionado());
     // ser_solidario.crearInforme("reportes/rep_solidario/sub_sub_rep_solidario.jasper", parametros);
      //ser_solidario.verVisor();
      try {
         /*   String cadenaConexion = "jdbc:oracle:thin:@servidor.prueba.co:1521:BASEDATOS";
            Class.forName("oracle.jdbc.OracleDriver");
            Connection connection = DriverManager.getConnection(cadenaConexion,
                    "usuarioBD", "clave");*/
           /* Map args = new HashMap();
            String logoEtiqueta = FacesContext.getCurrentInstance().getExternalContext().
                    getRealPath("/vista/reportes/activosfijos/logoEtiqueta.png");*/
            String path = FacesContext.getCurrentInstance().getExternalContext().
                    getRealPath("/reportes/rep_solidario/sub_sub_rep_solidario.jasper");
            JasperPrint reporte = JasperFillManager.fillReport(path, parametros);
            HttpServletResponse httpServletResponse = (HttpServletResponse) FacesContext.getCurrentInstance().
                    getExternalContext().getResponse();
            String rutaJR = "C:\\bancos\\sam\\sam-war\\web\\reportes\\rep_solidario\\sub_sub_rep_solidario.jrxml";
            String archivo =  "prueba" +  ".pdf";
            String rutaDestino = "C:\\saes\\ejemplo.pdf";
            httpServletResponse.addHeader("Content-disposition", "attachment; filename=" + archivo);
            ServletOutputStream servletOutputStream = httpServletResponse.getOutputStream();
            JasperReport JasperReport = JasperCompileManager.compileReport(rutaJR);
            JasperExportManager.exportReportToPdfFile(reporte, rutaDestino);
            
            FacesContext.getCurrentInstance().responseComplete();
          } catch (Exception e) {
            System.err.println( "Error iReport: " + e.getMessage() );
        }


    }
    
    @Override
    public void insertar() {
        tab_tabla.insertar();
    }

    @Override
    public void guardar() {
        if (tab_tabla.guardar()) {
            guardarPantalla();
        }
    }

    @Override
    public void eliminar() {
        tab_tabla.eliminar();
    }

    public Tabla getTab_tabla() {
        return tab_tabla;
    }

    public void setTab_tabla(Tabla tab_tabla) {
        this.tab_tabla = tab_tabla;
    }

    public Calendario getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Calendario fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Calendario getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Calendario fechaFin) {
        this.fechaFin = fechaFin;
    }

    public VisualizarPDF getVipdf_comprobante() {
        return vipdf_comprobante;
    }

    public void setVipdf_comprobante(VisualizarPDF vipdf_comprobante) {
        this.vipdf_comprobante = vipdf_comprobante;
    }

    public Reporte getRep_reporte() {
        return rep_reporte;
    }

    public void setRep_reporte(Reporte rep_reporte) {
        this.rep_reporte = rep_reporte;
    }

    public SeleccionFormatoReporte getSel_rep() {
        return sel_rep;
    }

    public void setSel_rep(SeleccionFormatoReporte sel_rep) {
        this.sel_rep = sel_rep;
    }

    public Map getMap_parametros() {
        return map_parametros;
    }

    public void setMap_parametros(Map map_parametros) {
        this.map_parametros = map_parametros;
    }

    public VisualizarPDF getVipdf_contrato() {
        return vipdf_contrato;
    }

    public void setVipdf_contrato(VisualizarPDF vipdf_contrato) {
        this.vipdf_contrato = vipdf_contrato;
    }
    
}
