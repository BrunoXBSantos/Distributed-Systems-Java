import java.util.Calendar; 
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;


class ClientData{
	private String password;
	private Calendar registerDate;

	public ClientData(String password, Calendar registerDate){
		this.password=password;
		this.registerDate=new GregorianCalendar();
	}

	// devolve a password
	public String getPassword(){
		return this.password;
	}

	public Calendar getRegisterDate(){
		return this.registerDate;   // this.registerDate.clone();
  	}

}

class Server{
	public static void main(String[] args){

		HashMap<String, ClientData> clientsList = new HashMap<>();  // lista de clientes

		// verificacao do numero de parametros de args
		// É necessario a introduzao do numero da porta
		if(args.length != 1){
			System.out.println("Error! Necessário introduzir o numero da porta.");
			System.exit(1);
		}

		try{
			int port = Integer.parseInt(args[0]);  // string para int do num da porta
		}
		catch(NumberFormatException e){
			System.out.println("Numero da porta tem de ser um numero inteiro");

			//e.printStackTrace();
		}

		ServerSocket listener = new ServerSocket(port);  // socket, bind e listener


		while(true){
			Socket socket = listener.accept();
		}

	}


}


// primeira mensagem do cliente é a autenticacao?
// depois o cliente comunica quantos casos ..
// o servidor envia a todos os clientes liagados uma nova estimativa