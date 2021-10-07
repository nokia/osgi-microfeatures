package alcatel.tess.hometop.gateways.utils;

import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Gauge;
import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.MeterListener;
import com.alcatel.as.service.metering.MeteringService;
import com.alcatel.as.service.metering.Rate;
import com.alcatel.as.service.metering.Sampler;
import com.alcatel.as.service.metering.Stat;
import com.alcatel.as.service.metering.StopWatch;

public class NullMeteringService implements MeteringService {
  private final static MeteringService _instance = new NullMeteringService();
  
  public static MeteringService getInstance() {
    return _instance;
  }
  
  private final Gauge _nullGauge = new Gauge() {
    @Override
    public void removeMeterListener(MeterListener listener) {
    }
    
    @Override
    public String getName() {
      return "";
    }
    
    @Override
    public Sampler createSampler() {
      return null;
    }
    
    @Override
    public Object attachment() {
      return null;
    }
    
    @Override
    public void attach(Object attachment) {
    }
    
    @Override
    public void addMeterListener(MeterListener listener) {
    }
    
    @Override
    public void set(long value) {
    }
    
    @Override
    public void add(long delta) {
    }

    @Override
    public long getValue() {
      return 0;
    }

    @Override
    public boolean hasListeners() {
      return false;
    }
  };
  
  private StopWatch _nullStopWatch = new StopWatch() {
    @Override
    public long stop() {
      return 0;
    }
    
    @Override
    public void resume() {
    }
    
    @Override
    public long pause() {
      return 0;
    }
    
    @Override
    public long getElapsedTime() {
      return 0;
    }
    
    @Override
    public Counter getCounter() {
      return _nullCounter;
    }

	@Override
	public void close() {
	}
  };
  
  private final Counter _nullCounter = new Counter() {
    @Override
    public void removeMeterListener(MeterListener listener) {
    }
    
    @Override
    public String getName() {
      return "";
    }
    
    @Override
    public Sampler createSampler() {
      return null;
    }
    
    @Override
    public Object attachment() {
      return null;
    }
    
    @Override
    public void attach(Object attachment) {
    }
    
    @Override
    public void addMeterListener(MeterListener listener) {
    }
    
    @Override
    public StopWatch start() {
      return _nullStopWatch;
    }
    
    @Override
    public void add(long value) {
    }

    @Override
    public long getValue() {
      return 0;
    }

    @Override
    public boolean hasListeners() {
      return false;
    }
  };
  
  protected Sampler _nullSampler = new Sampler() {
    @Override
    public void meterChanged(Meter meter, long newValue, boolean add) {
    }
    
    @Override
    public void remove() {
    }
    
    @Override
    public Meter getMeter() {
      return null;
    }
    
    @Override
    public Stat computeStat() {
      return null;
    }
  };
  
  private Rate _nullRate = new Rate() {
    @Override
    public void removeMeterListener(MeterListener listener) {
    }
    
    @Override
    public String getName() {
      return "";
    }
    
    @Override
    public Sampler createSampler() {
      return _nullSampler;
    }
    
    @Override
    public Object attachment() {
      return null;
    }
    
    @Override
    public void attach(Object attachment) {
    }
    
    @Override
    public void addMeterListener(MeterListener listener) {
    }
    
    @Override
    public void hit(long hits) {
    }
    
    @Override
    public void hit() {
    }

    @Override
    public long getValue() {
      return 0;
    }

    @Override
    public boolean hasListeners() {
      return false;
    }
  };
  
  @Override
  public Gauge getGauge(String name) {
    return _nullGauge;
  }
  
  @Override
  public Counter getCounter(String name) {
    return _nullCounter;
  }
  
  @Override
  public Rate getRate(String name) {
    return _nullRate;
  }
}
