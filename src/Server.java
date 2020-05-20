import java.util.Calendar; 
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Set;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.StringTokenizer;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;



class ClientHandler implements Runnable{
	public static String CSI = "\u001B[";
	private Socket socket;  // o socket devolvido por accept
	private DataBaseServer dataBaseServer;
	private LoggedClients loggedClients;
    private Integer id;

	public ClientHandler(Socket socket, DataBaseServer dataBaseServer, LoggedClients loggedClients, Integer id) {
        this.socket = socket;
        this.dataBaseServer = dataBaseServer;
        this.loggedClients = loggedClients;
        this.id = id;
    }

	public void run(){
		String name = null;
		String password = null;

		try{

		boolean connected = true;
		BufferedReader in = new BufferedReader(
			new InputStreamReader(this.socket.getInputStream()));
		boolean autoFlush = true;
		PrintWriter out = new PrintWriter(this.socket.getOutputStream(),autoFlush);

		String msgReceive =  in.readLine();
		if (msgReceive.equals("0")) {
            connected = false;
        }
        else{
			if(msgReceive.equals("1")){
				String authR = in.readLine();
	        	StringTokenizer st = new StringTokenizer(authR, " ");
				name = st.nextToken();
				password = st.nextToken();
				boolean existClient = this.dataBaseServer.existClient(name);
				if(existClient){
					// verifica a pass
					boolean checkPassword = this.dataBaseServer.checkPassword(name, password);
					if(checkPassword){
						// pass correta
						out.println("PC");  // pass correta
					}
					else{
						out.println("PI");  // pass incorreta
						connected = false;

					}
				}
				else{
					out.println("NE");  // cliente nao existe
					connected = false;
				}
			}
			else if (msgReceive.equals("2")) {
				String register = in.readLine();
	        	StringTokenizer st = new StringTokenizer(register, " ");
				name = st.nextToken();
				password = st.nextToken();
				boolean existClient = this.dataBaseServer.existClient(name);
                if (existClient) {
                    out.println("UE"); // utilizador já existe
                    connected = false;
                }
                else {
                    this.dataBaseServer.createClient(name, password, this.id);
                    out.println("RS");  // registado com sucesso
                }
            }
        }
        if(connected){
        	System.out.print (CSI + "32" + "m");
	  	 	System.out.print ("\t" + name.toUpperCase() + " autenticado com sucesso!\n");
	    	System.out.println (CSI + "m");
	    	this.loggedClients.connect(this.id, out);
	    }

		while(connected){
			String msg = in.readLine();
			if(msg == null || msg.equals("quit")){
				connected = false;
			}
			else{
				int cases = 0;
				boolean flagC = false;
				do{
					try{
						cases = Util.verifyCases(msg,this.dataBaseServer.getContacts(name));
						flagC = true;
					}
					catch(NumberFormatException e){
						out.println(e.getMessage());
						msg = in.readLine();
					}
					catch(NumberInvalidException e){
						out.println(e.getMessage());
						msg = in.readLine();
					}	
				}
				while(!flagC);

				this.dataBaseServer.setReportedCases(name, Integer.parseInt(msg));
				float avgProportion = (float) this.dataBaseServer.avgProportion();
				String avgProportionS = String.format("%.2f",avgProportion * 100);
				String msgSend = new String("Average Proportion Atual: " /*+ avgProportion + " -> "*/ + avgProportionS + "%");
				this.loggedClients.sendAll(this.id, msgSend);
				
				System.out.print (CSI + "38" + "m");
				System.out.print("\t" + name + ": " + msg + "/150");
    			System.out.println (CSI + "m");
    			System.out.print (CSI + "31" + "m");
  	 			System.out.print ("\t" + msgSend + "\n");
    			System.out.println (CSI + "m");
			}
		}

		this.loggedClients.disconnect(this.id);

		System.out.print (CSI + "31" + "m");		
		System.out.println("\n\t" + name.toUpperCase() + " Disconnected");
		System.out.println (CSI + "m");	
	
		this.socket.shutdownInput();
		this.socket.shutdownOutput();
		this.socket.close();

		}

		catch(Exception e){

		}
	}
}


class Connection {
	private PrintWriter out;

	public Connection(PrintWriter out) {
		this.out = out;
	}

	synchronized void send(String message) {
		this.out.println(message);
	}
}


class LoggedClients{
    private HashMap<Integer, Connection> logged;
    
    public LoggedClients() {
        this.logged = new HashMap<Integer, Connection>();
    }
    
    public synchronized void connect(Integer id, PrintWriter out) {
        Connection connection = new Connection(out);
        this.logged.put(id, connection);
    }
    
    public synchronized void disconnect(Integer id) {
        this.logged.remove(id);
    }

    public synchronized void sendAll(Integer id, String message) {
	    for(Integer idClient: this.logged.keySet()) {
	        Connection connection = this.logged.get(idClient);
	    	SendClient sendClient = new SendClient(connection, id, message);
	    	Thread thread = new Thread(sendClient);
	    	thread.start();
	    }
  	}
}

// thread para enviar para um cliente
class SendClient implements Runnable{
	private Connection connection;
	private int id;
	private String message;
	private Calendar date;

	SendClient(Connection connection, int id, String message){
		this.connection = connection;
		this.id = id;
		this.message = message;
		this.date = new GregorianCalendar();
	}

	public void run(){
		String dateString = String.format("%1$tY/%1$tm/%1$td %tT",this.date);
		this.connection.send(id + ": " + message + "\t     " + dateString);
	}
}


class Server{
	public static void main(String[] args){

		DataBaseServer dataBaseServer = new DataBaseServer("Server");
		LoggedClients loggedClients = new LoggedClients(); // recebe casos dos clientes
		String CSI = "\u001B[";   // cores no terminal
		ServerSocket listener = null;
		int port = 3001;
		int nextId = 1;

		// verificacao do numero de parametros de args
		// É necessario a introduzao do numero da porta
		if(args.length != 1){
			System.out.println("Error! Necessário introduzir o numero da porta.");
			System.exit(1);
		}

		try{
			port = Util.verifyPort(args[0]);  // string para int do num da porta
		}
		catch(NumberPortInvalidException e){
			System.out.println("Error! " + e.getMessage());
			System.exit(1);
		}
		catch(NumberFormatException e){
			System.out.println("Error! " + e.getMessage());
			System.exit(1);
		}

		try{
			listener = new ServerSocket(port);  // socket, bind e listener
		}
		catch(IOException e){
			System.out.println("Error creating the socket!");
			System.exit(1);
		}


		System.out.print (CSI + "44" + "m");
		System.out.println("\n\tServer initialized: " + listener.getLocalSocketAddress());
    	System.out.print(CSI + "m");
		System.out.print("\n");

		boolean flag = true;

		while(flag){
			try{
				Socket socket = listener.accept();
				System.out.print (CSI + "32" + "m");
				System.out.println("\tNew connection");
				System.out.print(CSI + "m");

				// criacao das threads
				ClientHandler client = new ClientHandler(socket, dataBaseServer, loggedClients, nextId);
				Thread clientThread = new Thread(client);
				
				//inicio
				clientThread.start();

				nextId++; 
				
				if("quit".equals("or")){
					flag = false;
				}
			}	
			catch(IOException e){
				System.out.println("Error accepting the connection");
			}	
		}

		try{
			listener.close();
		}
		catch(IOException e){
			System.out.println("Error closing listener");
			System.exit(1);
		}		
	}


}

class NumberPortInvalidException extends Exception{
	NumberPortInvalidException(String s){
		super(s);
	}
}
class NumberInvalidException extends Exception{
	NumberInvalidException(String s){
		super(s);
	}
}

class Util{
	public static int verifyPort(String s) throws NumberFormatException, NumberPortInvalidException{
		try{
			int n = Integer.parseInt(s);
			if(n<3000){
				throw new NumberPortInvalidException("Numero da porta Tem de ser maior que 3000");
			}		
			else
				return n;
		}
		catch(NumberFormatException e){
			throw new NumberFormatException("Porta tem de ser um número inteiro");
		}
		
	}

	public static int verifyNumberOption(String s) throws NumberFormatException, NumberInvalidException {
		int n = 0;
		try{
			n = Integer.parseInt(s);
			if(n == 1 || n == 0){
				return n;
			}
			else{
				throw new NumberInvalidException("Opcao tem de ser 0 ou 1");
			}
		}
		catch(NumberFormatException e){
			throw new NumberFormatException("Numero tem de ser um inteiro");
		}
	}

	public static int verifyCases(String msg, int contacts) throws NumberFormatException, NumberInvalidException{
		int cases = 0;
		try{
			cases = Integer.parseInt(msg);
			if(cases >= 0 && cases <= contacts){
				return cases;
			}
			else if(cases < 0 )
				throw new NumberInvalidException("Número de casos não pode ser negativo!");
			else if(cases > contacts)
				throw new NumberInvalidException("Número de casos não pode ser superior aos contactos!");
			else{
				throw new NumberInvalidException("Erro no numero de casos");
			}
		}
		catch(NumberFormatException e){
			throw new NumberFormatException("Numero tem de ser um inteiro");
		}
	}
}