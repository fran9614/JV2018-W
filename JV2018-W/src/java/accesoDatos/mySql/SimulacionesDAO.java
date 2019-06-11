/**  
 * Proyecto: Juego de la vida.
 *  Resuelve todos los aspectos del almacenamiento del
 *  DTO Usuario utilizando base de datos mySQL.
 *  Colabora en el patron Fachada.
 *  @since: prototipo2.2
 *  @source: SimulacionesDAO.java  
 *  @version: 2.2 - 2019/06/05  
 *  @author: ajp
 *  @author: VictorJLucas
 */
 
package accesoDatos.mySql;
 
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
 
import java.sql.Connection;
 
import accesoDatos.DatosException;
import accesoDatos.OperacionesDAO;
import config.Configuracion;
import modelo.ClaveAcceso;
import modelo.Correo;
import modelo.DireccionPostal;
import modelo.ModeloException;
import modelo.Mundo;
import modelo.Nif;
import modelo.Simulacion;
import modelo.Simulacion.EstadoSimulacion;
import modelo.Usuario;
import modelo.Usuario.RolUsuario;
import util.Fecha;
import util.Formato;
 
public class SimulacionesDAO implements OperacionesDAO {
    
    //1. Victor
 
    // Singleton
    private static SimulacionesDAO instance = null;
    private Connection db;
    private Statement stSimulaciones;
    private ResultSet rsSimulaciones;
    private DefaultTableModel tmSimulaciones;     // Tabla del resultado de la consulta.
    private ArrayList<Simulacion> bufferSimulaciones;     
 
    /**
     *  Método de acceso a la instancia única.
     *  Si no existe la crea invocando al constructor interno.
     *  Utiliza inicialización diferida de la instancia única.
     *  @return instancia
     */
    public static SimulacionesDAO getInstance() {
   	 if (instance == null) {
   		 instance = new SimulacionesDAO();
   	 }
   	 return instance;
    }
 
    /**
     * Constructor de uso interno.
     */
    private SimulacionesDAO() {
   		 inicializar();
    }
 
    /**
     * Inicializa el DAO, detecta si existen las tablas de datos capturando la
     * excepción SQLException.
     * @throws SQLException  
     */
    private void inicializar() {
   	 db = Conexion.getDB();
   	 try {
   		 crearTablaSimulaciones();
   		 stSimulaciones = db.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
   	 // Crea el tableModel y el buffer de objetos para usuarios.
   	 tmSimulaciones = new DefaultTableModel();
   	 bufferSimulaciones = new ArrayList<Simulacion>();  
    }
 
    /**
     * Crea la tabla de usuarios en la base de datos.
     * @throws SQLException  
     */
    private void crearTablaSimulaciones() throws SQLException {
   	 Statement s = db.createStatement();
   	 s.executeUpdate("CREATE TABLE IF NOT EXISTS simulaciones("  
   			 + "usr VARCHAR(45) NOT NULL ,"
   			 + "fecha DATE,"
   			 + "mundo VARCHAR(45) NOT NULL ,"
   			 + "ciclos CHAR(10) NOT NULL ,"  
   			 + "estado VARCHAR(20) NOT NULL ," 
   			+ "PRIMARY KEY (`usr`, `fecha`))");
   			 
    }
 
    // MÉTODOS DAO Usuarios
 
    /**
     * Obtiene el usuario buscado dado un objeto.  
     * Si no existe devuelve null.
     * @param Usuario a obtener.
     * @return (Usuario) buscado.  
     */    
    public Simulacion obtener(Object obj) {
   	 assert obj != null;
   	 return obtener(((Simulacion) obj).getId());
    }
 
    //1
    private void cargarPredeterminados() throws SQLException, DatosException {
    	try {
			alta(new Simulacion());
			alta(new Simulacion(new Usuario(),
					new Fecha(),new Mundo(),8,Simulacion.EstadoSimulacion.PREPARADA));
		} catch (ModeloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    //2. Jorge
    
    /**
     * Obtiene el usuario buscado dado su id, el nif o el correo.  
     * Si no existe devuelve null.
     * @param idUsr del usuario a obtener.
     * @return (Usuario) buscado.  
     */    
    @Override
    public Simulacion obtener(String id) {   	 
   	 assert id != null;
   	 assert !id.equals("");
   	 assert !id.equals(" ");
   	 
   	 ejecutarConsulta(id);
 
   	 // Establece columnas y etiquetas.
   	 establecerColumnasModelo();
 
   	 // Borrado previo de filas.
   	 borrarFilasModelo();
 
   	 // Volcado desde el resulSet.
   	 rellenarFilasModelo();
 
   	 // Actualiza buffer de objetos.
   	 sincronizarBufferUsuarios();
 
   	 if (bufferSimulaciones.size() > 0) {
   		 return (Simulacion) bufferSimulaciones.get(0);
   	 }
   	 return null;
    }
 
    /**
     * Determina el idUsr recibido y ejecuta la consulta.
     * Los resultados quedan en el ResultSet
     * @param idUsr
     */
    private void ejecutarConsulta(String idSimul) {
   	 try {
   		 rsSimulaciones = stSimulaciones.executeQuery("SELECT * FROM simulaciones " 
   		 	+ "WHERE CONCAT(usr,':',DATE_FORMAT(fecha,'%Y%m%d%H%i%s'))='"+ idSimul+"'");
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
    }
 
    /**
     * Crea las columnas del TableModel a partir de los metadatos del ResultSet
     * de una consulta a base de datos
     */
    private void establecerColumnasModelo() {
   	 try {
   		 // Obtiene metadatos.
   		 ResultSetMetaData metaDatos = this.rsSimulaciones.getMetaData();
 
   		 // Número total de columnas.
   		 int numCol = metaDatos.getColumnCount();
 
   		 // Etiqueta de cada columna.
   		 Object[] etiquetas = new Object[numCol];
   		 for (int i = 0; i < numCol; i++) {
   			 etiquetas[i] = metaDatos.getColumnLabel(i + 1);
   		 }
 
   		 // Incorpora array de etiquetas en el TableModel.
   		 ((DefaultTableModel) this.tmSimulaciones).setColumnIdentifiers(etiquetas);
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
    }
 
    /**
     * Borra todas las filas del TableModel
     * @param tm - El TableModel a vaciar
     */
    private void borrarFilasModelo() {
   	 while (this.tmSimulaciones.getRowCount() > 0)
   		 ((DefaultTableModel) this.tmSimulaciones).removeRow(0);
    }
 
    /**
     * Replica en el TableModel las filas del ResultSet
     */
    private void rellenarFilasModelo() {
   	 Object[] datosFila = new Object[this.tmSimulaciones.getColumnCount()];
   	 // Para cada fila en el ResultSet de la consulta.
   	 try {
   		 while (rsSimulaciones.next()) {
   			 // Se replica y añade la fila en el TableModel.
   			 for (int i = 0; i < this.tmSimulaciones.getColumnCount(); i++) {
   				 datosFila[i] = this.rsSimulaciones.getObject(i + 1);
   			 }
   			 ((DefaultTableModel) this.tmSimulaciones).addRow(datosFila);
   		 }
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
    }
 
    /**
     * Regenera lista de los objetos procesando el tableModel.  
     */    
    private void sincronizarBufferUsuarios() {
   	 bufferSimulaciones.clear();
   	 for (int i = 0; i < tmSimulaciones.getRowCount(); i++) {
   		 Usuario usr = new Usuario((Usuario) tmSimulaciones.getValueAt(i, 0));
   		 Fecha fecha = new Fecha((java.sql.Date)tmSimulaciones.getValueAt(i, 1));
   		 Mundo mundo = new Mundo((Mundo)tmSimulaciones.getValueAt(i, 2));
   		 int ciclos = (int) tmSimulaciones.getValueAt(i, 3);
   		 EstadoSimulacion estado = null;
   		 switch ((String) tmSimulaciones.getValueAt(i, 4)) {
   		 case "PREPARADA":   
   			 estado = EstadoSimulacion.PREPARADA;
   			 break;
   		 case "INICIADA":
   			 estado = EstadoSimulacion.INICIADA;
   			 break;
   		 case "COMPLETADA":
   			 estado = EstadoSimulacion.COMPLETADA;
   			 break;
   		 }    
   		 // Genera y guarda objeto
   		 bufferSimulaciones.add(new Simulacion(usr, fecha, mundo, ciclos, estado));
   	 }
    }
 
    public ArrayList<Simulacion> obtenerTodasMismoUsr(String idUsr) {
   	 assert idUsr != null;
   	 assert !idUsr.equals("");
   	 assert !idUsr.equals(" ");
   	 
   	 try {
   		 rsSimulaciones = stSimulaciones.executeQuery("SELECT * FROM simulaciones WHERE usr = '"+ idUsr + "'");
   		 this.establecerColumnasModelo();

   		 this.borrarFilasModelo();

   		 this.rellenarFilasModelo();

   		 this.sincronizarBufferUsuarios();   
   	 
   	 } catch (SQLException e) {
   		 e.printStackTrace();
   	 }
   	 
   	 return this.bufferSimulaciones;
    }
     
   
    
} // class





