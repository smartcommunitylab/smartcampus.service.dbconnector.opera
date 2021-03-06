package smartcampus.services.dbconnector.opera.script;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.apache.log4j.Logger;

import smartcampus.services.dbconnector.opera.data.message.Operadb.DBApertura;
import smartcampus.services.dbconnector.opera.data.message.Operadb.DBDish;
import smartcampus.services.dbconnector.opera.data.message.Operadb.DBMenu;
import smartcampus.services.dbconnector.opera.data.message.Operadb.DBOperaStudent;
import smartcampus.services.dbconnector.opera.data.message.Operadb.DBPayment;
import smartcampus.services.dbconnector.opera.data.message.Operadb.DBStudentPayment;
import smartcampus.services.dbconnector.opera.data.message.Operadb.DataMenu;
import smartcampus.services.dbconnector.opera.data.message.Operadb.DataOpening;
import smartcampus.services.dbconnector.opera.data.message.Operadb.DataOpeningCanteen;

import com.google.protobuf.Message;

public class OperaDBScript {

	Logger log = Logger.getLogger(this.getClass());

	public static String generateDate() {
		Date date = new Date(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000));
		return date.toString();
	}

	public static DBOperaStudent mergePayments(Collection<DBPayment> payments) {
		DBOperaStudent.Builder sbuilder = DBOperaStudent.newBuilder();
		boolean first = true;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.add(Calendar.MONTH, -1);

		String oma = sdf.format(cal.getTime()) + ".0";

		for (DBPayment payment : payments) {
			if (first) {
				sbuilder.setIdgiada(payment.getIdgiada());
				sbuilder.setNome(payment.getNome());
				sbuilder.setCognome(payment.getCognome());
				sbuilder.setIsunitn(payment.getIsunitn());
				sbuilder.setUidbadge(payment.getUidbadge());
				sbuilder.setConsumi(payment.getConsumi());
				sbuilder.setResiduo(payment.getResiduo());
				first = false;
			}

			if (payment.getDatapassaggio().compareTo(oma) < 0) {
				continue;
			}

			DBStudentPayment.Builder pbuilder = DBStudentPayment.newBuilder();
			pbuilder.setProfiloattuale(payment.getProfiloattuale());
			pbuilder.setProfilopassaggio(payment.getProfilopassaggio());
			pbuilder.setProgressivopassaggio(payment.getProgressivopassaggio());
			pbuilder.setDatapassaggio(payment.getDatapassaggio());
			pbuilder.setCodicemensa(payment.getCodicemensa());
			pbuilder.setDescrizionemensa(payment.getDescrizionemensa());
			pbuilder.setCodicepostazione(payment.getCodicepostazione());
			pbuilder.setDescrizioneprodotto(payment.getDescrizioneprodotto());
			pbuilder.setTipoprodotto(payment.getTipoprodotto());
			pbuilder.setImportototalepassaggio(payment.getImportototalepassaggio());
			pbuilder.setImportoprodottodettaglio(payment.getImportoprodottodettaglio());

			sbuilder.addPayments(pbuilder.build());
		}

		return sbuilder.build();
	}

	public List<Message> buildMenu(List<Message> dishes, List<Message> menus) {
		List<Message> result = new ArrayList<Message>();

		Map<String, DBDish> dMap = new TreeMap<String, DBDish>();
		MultiMap mMap = new MultiHashMap();
		// ArrayListM<String, DBDish> mMap = new TreeMap<String, DBDish>();

		for (Message d : dishes) {
			dMap.put(((DBDish) d).getId(), (DBDish) d);
		}

		for (Message m : menus) {
			DBMenu dbm = (DBMenu) m;
			mMap.put(dbm.getData() + "_" + dbm.getPasto(), (DBMenu) m);
		}

		for (Object key : mMap.keySet()) {
			List c = (List) mMap.get(key);
			DataMenu.Builder builder = DataMenu.newBuilder();
			for (Object m : c) {
				DBMenu menu = (DBMenu) m;
				builder.setDate(menu.getData());
				builder.setType(menu.getPasto());
				if (dMap.get(menu.getId()) != null) {
					builder.addDishes(dMap.get(menu.getId()));
				} else {
					log.warn("Missing dish : " + menu.getId());
				}
			}
			result.add(builder.build());
		}

		return result;
	}

	public List<Message> buildAperture(List<Message> openings) {
		List<Message> result = new ArrayList<Message>();

		MultiMap oMap = new MultiHashMap();
		Map<String, Message.Builder> cMap = new TreeMap<String, Message.Builder>();
		for (Message msg: openings) {
			DBApertura opening = (DBApertura)msg;
			
			DataOpeningCanteen.Builder builder = DataOpeningCanteen.newBuilder();
			builder.setDescrmensa(opening.getDescrmensa());

			
			Date startDate = Date.valueOf(opening.getData());
			Calendar cal = new GregorianCalendar();
			cal.setTime(startDate);
			String days = opening.getGiorni();
			for (int i = 0; i < days.length(); i++) {
			if (days.charAt(i) == 'a') {
					Date date = new Date(cal.getTimeInMillis());
					builder.addDate(date.toString());
				}
				
				cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 1);
			}
			
			String zone = opening.getDescrzona();
			oMap.put(zone, builder.build());
			if (!cMap.containsKey(zone)) {
				DataOpening.Builder builder2 = DataOpening.newBuilder();
				builder2.setDescrzona(zone);
				cMap.put(zone,builder2);
			}
			
//			result.add(builder.build());
		}
		
		for (String key: cMap.keySet()) {
			((DataOpening.Builder)cMap.get(key)).addAllOpenings((List<DataOpeningCanteen>)oMap.get(key));
			result.add(((DataOpening.Builder)cMap.get(key)).build());
		}
		
		return result;

	}

}
