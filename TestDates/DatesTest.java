import java.util.GregorianCalendar;
import java.util.Calendar;

class DatesTest{
	public static void main(String args[]){
		Calendar now = new GregorianCalendar();

		System.out.printf("%1$tY/%1$tm/%1$td %tT%n",now);
	}
}