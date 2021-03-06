package servicio;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.util.Vector;


public class GestorAlquileres {

	private final int NPUESTOS = 5; // Numero de puestos de alquiler de bicis
	private final int NBICIS = 10;  // Bicicletas por puesto

	private RandomAccessFile stream;


	/**
	 * Constructor del gestor de consultas del servicio de alquiler.
	 * Crea un fichero con datos de prueba
	 */
	public GestorAlquileres() {
		creaFichero("bicis.dat");
	}

	/**
	 * Cierra el flujo/stream asociado al fichero de comics.
	 * @throws IOException 
	 */
	public void cierraGestor() throws IOException{
		stream.close();
	}

	/**
	 * Si no existe, crea el fichero y lo rellena con unas bicis
	 * 
	 * @param nombreFichero
	 */
	public void creaFichero(String nombreFichero) {
		File f = new File(nombreFichero);
		stream = null;
		try {
			if (!f.exists()) {
				stream = new RandomAccessFile(f, "rw"); // al no existir, lo crea

				// Primero colocamos NBICIS/2 en cada puesto
				for (int p = 0; p < NPUESTOS; p++) {
					stream.writeInt(NBICIS/2);
				}				

				// Ahora llena la primera mitad de cada puesto almacenando bicis con hora de devolucion 00:00
				Bicicleta bici = null;
				int ct = 0;
				for (int p = 0; p < NPUESTOS; p++) {
					for (int b = 0; b < NBICIS/2; b++) {
						bici = new Bicicleta("B"+String.format("%03d", ct), p, "        ", 0, 0);
						bici.escribeEnFichero(stream); 
						ct++;
					}
				}	

			}	
			else
				stream = new RandomAccessFile(f, "rw"); // al existir, lo abre sin destruir su contenido
		}
		catch (IOException e) {
			System.out.println("Problema al crear el fichero");
			System.exit(0);
		};
	}
	

	/**
	 * Consulta el numero de bicis disponibles en todos los puestos
	 * 
	 * @return	vector con las bicis disponibles en cada puesto
	 * @throws IOException 
	 */
	public int[] consultaDisponibles() throws IOException {
		stream.seek(0);
		int[] nBicis = new int[NPUESTOS];
		for(int i=0; i<NPUESTOS; i++) {
			nBicis[i]=stream.readInt();
		}
		return nBicis; // DEVOLVER LA INFORMACION ADECUADA EN CADA CASO
	}
	

	/**
	 * Devuelve las bicis alquiladas por el cliente
	 * 
	 * @param codcli
	 * @return vector de bicis alquiladas. Vector vacio si no tiene ninguna
	 * @throws IOException 
	 */
	public Vector<String> consultaAlquiladas(String codcli) throws IOException {	
		boolean finalizado = false;
		Vector<String> alquiladas = new Vector<>();
		Bicicleta bici = null;
		//if (codcli.length()<8 || codcli.length()>8)
		try {
			stream.seek(NPUESTOS*Integer.BYTES);
			bici = new Bicicleta();
			while(!finalizado) {
				bici.leeDeFichero(stream);
		  		if(bici.getCodcli().compareTo(codcli)==0) {
		  			alquiladas.add(bici.getCodbici());
		  		}
			}
		}catch (EOFException e) {
			finalizado = true;
		}	
		return alquiladas; // DEVOLVER LA INFORMACION ADECUADA EN CADA CASO
	}




	/**
	 * Devuelve el indice de la primera bici del puesto en el fichero
	 * 
	 * @param puesto	puesto en el que buscar
	 * @return	indice en el fichero. -1 si el puesto no existe o no tiene bicis
	 */
	private long buscaBiciPuesto(int puesto) {
		boolean encontrada = false;
		long indice = -1;
		Bicicleta bici = null;
		if ( (puesto < 0) || (puesto > NPUESTOS) )
			return -1;
		else {
			bici = new Bicicleta();
			// Recorre todo el fichero buscando bicis del cliente
			try {
				stream.seek(NPUESTOS*Integer.BYTES);
				while(!encontrada) {
					indice = stream.getFilePointer();
					bici.leeDeFichero(stream);
					if ( bici.getPuesto() == puesto ) 
						encontrada = true;
				}
			}
			catch (EOFException e) { // Hemos llegado al final del fichero
				indice = -1;
			}
			catch (IOException e) {
				indice = -1;
				System.out.println("Error en buscaBiciPuesto");
			}
			return indice;
		}	
	}



	/**
	 * El cliente codcli alquila una bici de un puesto dado
	 * 
	 * @param puesto	puesto del que alquilar
	 * @param codcli	codigo del cliente que alquila
	 * @return	bici alquilada. null si no hay bicis en ese puesto o el puesto no existe
	 * @throws IOException 
	 */
	public String alquilaBici(int puesto, String codcli) throws IOException {
		long indice = buscaBiciPuesto(puesto-1);
		int[] huecos = consultaDisponibles();
		LocalDateTime localDate = LocalDateTime.now();
		int hora = localDate.getHour();
		int minuto = localDate.getMinute();
		String codigo;
		if(indice==-1) codigo=null;
		else {
			Bicicleta bici = new Bicicleta();
			stream.seek((puesto-1)*Integer.BYTES);
			//Restamos una bici al puesto
			stream.writeInt(huecos[puesto-1]-1);
			//Buscamos la primera bici de ese puesto
			stream.seek(indice);
			bici.leeDeFichero(stream);
			bici.setCodcli(codcli);
			bici.setPuesto(-1);
			bici.setHora(hora);
			bici.setMinuto(minuto);
			stream.seek(indice);
			bici.escribeEnFichero(stream);
			codigo = bici.getCodbici();
		}
		return codigo; // DEVOLVER LA INFORMACION ADECUADA EN CADA CASO
	}



	/**
	 * Devuelve el numero de huecos en un puesto dado
	 * 
	 * @param puesto	puesto en el que buscar
	 * @return	numero de huecos hueco. -1 si el puesto no existe
	 * @throws IOException 
	 */
	private int numHuecos(int puesto) throws IOException {
		int huecos;
		if (puesto<1 || puesto > NPUESTOS)
			huecos = -1;
		else {
			stream.seek((puesto-1)*Integer.BYTES);
			int bicis = stream.readInt();
			huecos = NBICIS-bicis;
		}
		return huecos; // DEVOLVER LA INFORMACION ADECUADA EN CADA CASO
		
	}

	/**
	 * Devuelve la posicion en el fichero de la bici con un codigo dado
	 * 
	 * @param codbici	codigo de la bici
	 * @return	indice en el fichero. -1 si no se encuentra la bici
	 * @throws IOException 
	 */
	private long buscaBici(String codbici) throws IOException {
		boolean encontrada = false;
		long posicion = -1;
		stream.seek(NPUESTOS*Integer.BYTES);
		Bicicleta bici = new Bicicleta();
		try {
			while(!encontrada) {
				posicion=stream.getFilePointer();
				bici.leeDeFichero(stream);
				if(bici.getCodbici().compareTo(codbici)==0) {
					encontrada=true;
				}
			}
		}
		catch (EOFException e) { // Hemos llegado al final del fichero
			posicion = -1;
		}
		return posicion; // DEVOLVER LA INFORMACION ADECUADA EN CADA CASO
	}


	/**
	 * Devuelve una bici en un puesto dado
	 * 
	 * @param puesto	puesto en el que devolver la bici
	 * @param codbici	codigo de la bici
	 * @param codcli	codigo del cliente que devuelve la bici
	 * @return	bici devuelta. null si no la ha podido devolver
	 * @throws IOException 
	 */
	public String devuelveBici(int puesto, String codbici, String codcli) throws IOException {
		int[] disp = consultaDisponibles();
		long indice = buscaBici(codbici); 
		String devuelta=null;
		LocalDateTime localDate = LocalDateTime.now();
		int hora = localDate.getHour();
		int minuto = localDate.getMinute();
		if((puesto>=1 && puesto<=NPUESTOS)) {
			if(indice!=-1 && disp[puesto-1]<10) {
				Bicicleta bici = new Bicicleta();
				stream.seek((puesto-1)*Integer.BYTES);
				//Sumamos una bici al puesto
				stream.writeInt(disp[puesto-1]+1);
				stream.seek(indice);
				bici.leeDeFichero(stream);
				//Comprobamos que la bici encontrada esta alquilada por este cliente
				if(bici.getCodcli().compareTo(codcli)==0){
					bici.setPuesto(puesto-1);
					bici.setCodcli("        ");
					bici.setHora(hora);
					bici.setMinuto(minuto);
					stream.seek(indice);
					bici.escribeEnFichero(stream);
					devuelta=codbici;
				} 
			}
		}
		return devuelta; // DEVOLVER LA INFORMACION ADECUADA EN CADA CASO
	}
}
