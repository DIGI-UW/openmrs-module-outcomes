package org.openmrs.module.outcomes.api.resource;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QuestionaireResource implements Serializable {
	
	public String guid;
	
	public String openTightJar;
	
	public String heavyHouseholdChores;
	
	public String carryShoppingBag;
	
	public String washYourBack;
	
	public String useKnifeToCutFood;
	
	public String impactfulRecreationalActivities;
	
	public String interferenceWithSocialActivities;
	
	public String limitationsInWorkActivities;
	
	public String armShoulderHandPain;
	
	public String tinglingPinsAndNeedles;
	
	public String difficultySleeping;
	
	public Photo[] photo;
	
	public String vasPainScale;
	
	public String generalHealth;
	
	public String moderateEfforts;
	
	public String climbFloors;
	
	public String accomplishedLess;
	
	public String stoppedDoingWorkDaily;
	
	public String lessAccomplishedThanYouWouldLikeEmotion;
	
	public String unableToDoWorkOrOtherActivitiesCarefullyAsUsual;
	
	public String amountOfPainUnableToDoWork;
	
	public String haveYouFeltCalmAndPeaceful;
	
	public String hadALotOfEnergy;
	
	public String feltDiscouragedAndSad;
	
}
