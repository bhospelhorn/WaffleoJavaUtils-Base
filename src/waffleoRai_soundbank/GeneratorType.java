package waffleoRai_soundbank;

public enum GeneratorType {
	
	REVERB,
	VIBRATO_DELAY,
	VIBRATO,
	PORTAMENTO,
	PORTAMENTO_DELAY,
	
	START_SAMPLE_OVERRIDE,
	END_SAMPLE_OVERRIDE,
	LOOP_START_OVERRIDE,
	LOOP_END_OVERRIDE,
	START_SAMPLE_COARSE_OVERRIDE,
	MOD_LFO_PITCH,
	MOD_ENV_PITCH,
	HIGH_PASS_FILTER_FREQ,
	HIGH_PASS_FILTER_Q,
	MOD_LFO_HPF_FREQ,
	MOD_ENV_HPF_FREQ,
	END_SAMPLE_COARSE_OVERRIDE,
	MOD_LFO_VOL,
	CHORUS,
	PAN_OVERRIDE,
	
	MOD_LFO_DELAY,
	MOD_LFO_FREQ,
	VIB_LFO_DELAY,
	VIB_LFO_FREQ,
	MOD_ENV_DELAY,
	MOD_ENV_ATTACK,
	MOD_ENV_HOLD,
	MOD_ENV_DECAY,
	MOD_ENV_SUSTAIN,
	MOD_ENV_RELEASE,
	KEYNUM_TO_MOD_ENV_HOLD,
	KEYNUM_TO_MOD_ENV_DECAY,
	
	VOL_ENV_DELAY,
	VOL_ENV_ATTACK_OVERRIDE,
	VOL_ENV_HOLD,
	VOL_ENV_DECAY_OVERRIDE,
	VOL_ENV_SUSTAIN_OVERRIDE,
	VOL_ENV_RELEASE_OVERRIDE,
	KEYNUM_TO_VOL_ENV_HOLD,
	KEYNUM_TO_VOL_ENV_DECAY,
	
	KEY_RANGE_OVERRIDE,
	VEL_RANGE_OVERRIDE,
	LOOP_START_COARSE_OVERRIDE,
	CONSTANT_KEY,
	CONSTANT_VEL,
	VOLUME_OVERRIDE,
	LOOP_END_COARSE_OVERRIDE,
	
	TUNING_OVERRIDE_COARSE,
	TUNING_OVERRIDE_FINE,

	LOOP_TYPE_OVERRIDE,
	SCALE_TUNE,
	GREEDY,
	UNITY_KEY_OVERRIDE;

}
