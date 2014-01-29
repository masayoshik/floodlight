package net.floodlightcontroller.handover;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.util.HexString;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.devicemanager.internal.Device;

/*
*  Module monitoring device moves and removes their associated forwarding entries on the previous 
*  attachment point. Devices connected to OpenFlow switch via wire do not need this, b/c device move
*  generats a port down message which does the same thing. However, devices that move between WiFi access
*  point need help from this module to do handover.
*
* @Author Masayoshi Kobayashi (ON.Lab)
*/

public class Handover implements IDeviceListener, IFloodlightModule {

	protected static Logger logger;
	protected IFloodlightProviderService floodlightProvider;
	protected IDeviceService deviceservice;

	@Override
	public String getName() {
		return Handover.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(String type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(String type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void deviceAdded(IDevice device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deviceRemoved(IDevice device) {
		// TODO Auto-generated method stub

	}
	@Override
	public void deviceMoved(IDevice device) {
		long mac = device.getMACAddress();
		
		SwitchPort pap = null; // previous attachment point
		
		// If device is Device instance, we can use prevAP info
		if ( device instanceof Device ){ 
			pap = ((Device) device).getPrevAP();
			logger.debug("use prevAP {}", pap);
		}else{
			logger.info("Cannot support handover (prevAP is not available)");
			return;
		}

		if (pap == null){
			logger.info("Previous Attachement Point is null. MacAddress {}", HexString.toHexString(Ethernet.toByteArray(mac)));
			return;
		}
		logger.info("Detected Host (mac="+  HexString.toHexString(Ethernet.toByteArray(mac)) +") moved from {} to {}", pap, device.getAttachmentPoints()[0]);

		// Delete flow entries that have the device's mac address in dl_src or dl_dst
		IOFSwitch target_sw = floodlightProvider.getSwitch(pap.getSwitchDPID());
		if (target_sw != null){
			logger.info("Remove flow entries with mac address {} (src or dst) at Attachment Point {}", HexString.toHexString(Ethernet.toByteArray(mac)), pap);
			
			OFMatch match = new OFMatch();
			int wildcards = 0;
			match.setDataLayerDestination(Ethernet.toByteArray(mac));
			wildcards = OFMatch.OFPFW_ALL;
			wildcards &= ~ OFMatch.OFPFW_DL_DST;
			match.setWildcards(wildcards);
			OFMessage fm = ((OFFlowMod) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.FLOW_MOD)).setMatch(match)
				.setCommand(OFFlowMod.OFPFC_DELETE)
				.setLength(U16.t(OFFlowMod.MINIMUM_LENGTH));
			try {
				List<OFMessage> msglist = new ArrayList<OFMessage>(1);
				msglist.add(fm);
				target_sw.write(msglist, null);
			} catch (Exception e) {
				logger.error("Failed to clear flows on switch {} - {}", this, e);
			}
		
			match = new OFMatch();
			wildcards = 0;
			match.setDataLayerSource(Ethernet.toByteArray(mac));
			wildcards = OFMatch.OFPFW_ALL;
			wildcards &= ~ OFMatch.OFPFW_DL_SRC;
			match.setWildcards(wildcards);
			fm = ((OFFlowMod) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.FLOW_MOD)).setMatch(match)
				.setCommand(OFFlowMod.OFPFC_DELETE)
				.setLength(U16.t(OFFlowMod.MINIMUM_LENGTH));
			try {
				List<OFMessage> msglist = new ArrayList<OFMessage>(1);
				msglist.add(fm);
				target_sw.write(msglist, null);
			} catch (Exception e) {
				logger.error("Failed to clear flows on switch {} - {}", this, e);
			}
		}
		return;
	}
	
	@Override
	public void deviceIPV4AddrChanged(IDevice device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deviceVlanChanged(IDevice device) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l =
			new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		deviceservice = context.getServiceImpl(IDeviceService.class);
		deviceservice.addListener(this);

		logger = LoggerFactory.getLogger(Handover.class);
		logger.debug("Handover Init");
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		logger.debug("Handover Startup");
	}

}
