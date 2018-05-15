/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package securechat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author Federico Rossi
 */
public class SharedState {
    private boolean requestResponse = false;
    private boolean responseAvailable = false;
    private Lock requestLock = new ReentrantLock();
    private Condition responseAvailableCondition = requestLock.newCondition();
    private boolean handshakeProtocolTerminated = false;
    private boolean handshakeProtocolTerminationStatus = false;
    private Lock hpStatusLock = new ReentrantLock();
    private Condition handshakeStatusAvailable = hpStatusLock.newCondition();
    private static SharedState _instance;
    private SimpleBooleanProperty pendingRequest = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty isConnected = new SimpleBooleanProperty(false);
    public BooleanBinding sendBinding = Bindings.createBooleanBinding(() -> !isConnected.getValue(), isConnected);
    public BooleanBinding disconnectBinding = Bindings.createBooleanBinding(() -> !isConnected.getValue(), isConnected);
    public BooleanBinding connectBinding = Bindings.createBooleanBinding(() -> isConnected.getValue(), isConnected);
    public BooleanBinding acceptBinding = Bindings.createBooleanBinding(() -> !pendingRequest.getValue(), pendingRequest);
    
    private SharedState() {}
    
    public static SharedState getInstance() {
        if(_instance == null) _instance = new SharedState();
        return _instance;
    }
    
    public boolean waitForResponse() {
        requestLock.lock();
        try {
            long curTime = System.currentTimeMillis();
            long remained = 9000;
            long expires = curTime+remained;
            while(!responseAvailable) {
                responseAvailableCondition.await(remained,TimeUnit.MILLISECONDS);
                remained = expires-System.currentTimeMillis();
                if(remained<=0 && !responseAvailable){
                    //protocolDone(false); graphical feedback to the user?
                    break;
                }
            }
            System.out.println("10S ELPAPSED OR AN ANSWER WAS GIVEN--sharedstate");
            responseAvailable = false;
        } catch(Exception e) {e.printStackTrace(); return false;}
        finally {requestLock.unlock();}
        return requestResponse;
    }
    
    public boolean waitProtocol() {
        hpStatusLock.lock();
        try {
            while(!handshakeProtocolTerminated) {handshakeStatusAvailable.await();}
        } catch(Exception e ) {e.printStackTrace(); return false;}
        finally {hpStatusLock.unlock();}
        return handshakeProtocolTerminationStatus;
    }
    
    public void protocolDone(boolean result) {
        hpStatusLock.lock();
        try {
            setPendingRequest(false);
            handshakeProtocolTerminated=true;
            handshakeProtocolTerminationStatus=result;
            System.out.println("Signalling...");
            handshakeStatusAvailable.signal();
        } catch(Exception e) {e.printStackTrace();}
        finally{hpStatusLock.unlock();}
    }
    
    
    public void setResponse(boolean response) {
        requestLock.lock();
        try {
            responseAvailable=true;
            requestResponse=response;
            System.out.println("STATE: Response set, now signalling");
            responseAvailableCondition.signal();
        } catch(Exception e) {e.printStackTrace();}
        finally {requestLock.unlock();}
    }
    
    public boolean isRequestPending() {
        return pendingRequest.get();
    }
    
    public void setPendingRequest(boolean state) {
        pendingRequest.set(state);
    }
    
    public void setConnected(boolean v) {
        isConnected.set(v);   
    }
    
    
}
