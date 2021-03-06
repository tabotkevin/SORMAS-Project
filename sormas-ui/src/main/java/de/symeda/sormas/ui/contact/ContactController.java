/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.contact;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseCriteria;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseIndexDto;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactIndexDto;
import de.symeda.sormas.api.contact.ContactRelation;
import de.symeda.sormas.api.contact.FollowUpStatus;
import de.symeda.sormas.api.contact.SimilarContactDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.region.DistrictReferenceDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.caze.CaseContactsView;
import de.symeda.sormas.ui.caze.CaseSelectionField;
import de.symeda.sormas.ui.epidata.ContactEpiDataView;
import de.symeda.sormas.ui.epidata.EpiDataForm;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent.CommitListener;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.ViewMode;

public class ContactController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public ContactController() {

	}

	public void registerViews(Navigator navigator) {
		navigator.addView(ContactsView.VIEW_NAME, ContactsView.class);
		navigator.addView(ContactDataView.VIEW_NAME, ContactDataView.class);
		navigator.addView(ContactPersonView.VIEW_NAME, ContactPersonView.class);
		navigator.addView(ContactVisitsView.VIEW_NAME, ContactVisitsView.class);
		navigator.addView(ContactEpiDataView.VIEW_NAME, ContactEpiDataView.class);
	}

	public void create() {
		create(null);
	}

	public void create(CaseReferenceDto caseRef) {

		CaseDataDto caze = null;
		if (caseRef != null) {
			caze = FacadeProvider.getCaseFacade().getCaseDataByUuid(caseRef.getUuid());
		}
		CommitDiscardWrapperComponent<ContactCreateForm> createComponent = getContactCreateComponent(caze);
		VaadinUiUtil.showModalPopupWindow(createComponent, I18nProperties.getString(Strings.headingCreateNewContact));
	}

	public void navigateToData(String contactUuid) {
		navigateToData(contactUuid, false);
	}

	public void navigateToData(String contactUuid, boolean openTab) {
		String navigationState = ContactDataView.VIEW_NAME + "/" + contactUuid;
		if (openTab) {
			SormasUI.get().getPage().open(SormasUI.get().getPage().getLocation().getRawPath() + "#!" + navigationState, "_blank", false);
		} else {
			SormasUI.get().getNavigator().navigateTo(navigationState);
		}
	}

	public void editData(String contactUuid) {
		String navigationState = ContactDataView.VIEW_NAME + "/" + contactUuid;
		SormasUI.get().getNavigator().navigateTo(navigationState);
	}

	public void overview() {
		String navigationState = ContactsView.VIEW_NAME;
		SormasUI.get().getNavigator().navigateTo(navigationState);
	}

	public void caseContactsOverview(String caseUuid) {
		String navigationState = CaseContactsView.VIEW_NAME + "/" + caseUuid;
		SormasUI.get().getNavigator().navigateTo(navigationState);
	}

	/**
	 * Update the fragment without causing navigator to change view
	 */
	public void setUriFragmentParameter(String contactUuid) {
		String fragmentParameter;
		if (contactUuid == null || contactUuid.isEmpty()) {
			fragmentParameter = "";
		} else {
			fragmentParameter = contactUuid;
		}

		Page page = SormasUI.get().getPage();
		page.setUriFragment("!" + ContactsView.VIEW_NAME + "/" + fragmentParameter, false);
	}

	private ContactDto createNewContact(CaseDataDto caze) {
		ContactDto contact = caze != null ? ContactDto.build(caze) : ContactDto.build();

		UserReferenceDto userReference = UserProvider.getCurrent().getUserReference();
		contact.setReportingUser(userReference);

		return contact;
	}

	public CommitDiscardWrapperComponent<ContactCreateForm> getContactCreateComponent(CaseDataDto caze) {

		ContactCreateForm createForm = new ContactCreateForm(caze != null ? caze.getDisease() : null, caze != null);
		createForm.setValue(createNewContact(caze));
		final CommitDiscardWrapperComponent<ContactCreateForm> createComponent = new CommitDiscardWrapperComponent<ContactCreateForm>(
			createForm,
			UserProvider.getCurrent().hasUserRight(UserRight.CONTACT_CREATE),
			createForm.getFieldGroup());

		createComponent.addCommitListener(() -> {
			if (!createForm.getFieldGroup().isModified()) {
				final ContactDto dto = createForm.getValue();
				final PersonDto person = PersonDto.build();
				person.setFirstName(createForm.getPersonFirstName());
				person.setLastName(createForm.getPersonLastName());
				person.setNationalHealthId(createForm.getNationalHealthId());
				person.setPassportNumber(createForm.getPassportNumber());
				person.setBirthdateYYYY(createForm.getBirthdateYYYY());
				person.setBirthdateMM(createForm.getBirthdateMM());
				person.setBirthdateDD(createForm.getBirthdateDD());
				person.setSex(createForm.getSex());
				person.setPhone(createForm.getPhone());
				person.setEmailAddress(createForm.getEmailAddress());

				ControllerProvider.getPersonController()
					.selectOrCreatePerson(person, I18nProperties.getString(Strings.infoSelectOrCreatePersonForContact), selectedPerson -> {
						if (selectedPerson != null) {
							dto.setPerson(selectedPerson);

							// set the contact person's address to the one of the case when it is currently empty and
							// the relationship with the case has been set to living in the same household
							if (dto.getRelationToCase() == ContactRelation.SAME_HOUSEHOLD && dto.getCaze() != null) {
								PersonDto personDto = FacadeProvider.getPersonFacade().getPersonByUuid(selectedPerson.getUuid());
								if (personDto.getAddress().isEmptyLocation()) {
									CaseDataDto caseDto = FacadeProvider.getCaseFacade().getCaseDataByUuid(dto.getCaze().getUuid());
									personDto.getAddress().setRegion(caseDto.getRegion());
									personDto.getAddress().setDistrict(caseDto.getDistrict());
									personDto.getAddress().setCommunity(caseDto.getCommunity());
								}
								FacadeProvider.getPersonFacade().savePerson(personDto);
							}

							selectOrCreateContact(dto, person, I18nProperties.getString(Strings.infoSelectOrCreateContact), selectedContactUuid -> {
								if (selectedContactUuid != null) {
									editData(selectedContactUuid);
								}
							});
						}
					});
			}
		});

		return createComponent;
	}

	public void selectOrCreateContact(final ContactDto contact, final PersonDto personDto, String infoText, Consumer<String> resultConsumer) {
		ContactSelectionField contactSelect = new ContactSelectionField(contact, infoText, personDto.getFirstName(), personDto.getLastName());
		contactSelect.setWidth(1024, Unit.PIXELS);

		if (contactSelect.hasMatches()) {
			// TODO add user right parameter
			final CommitDiscardWrapperComponent<ContactSelectionField> component = new CommitDiscardWrapperComponent<>(contactSelect);
			component.addCommitListener(() -> {
				final SimilarContactDto selectedContact = contactSelect.getValue();
				if (selectedContact != null) {
					if (resultConsumer != null) {
						resultConsumer.accept(selectedContact.getUuid());
					}
				} else {
					createNewContact(contact, resultConsumer);
				}
			});

			contactSelect.setSelectionChangeCallback((commitAllowed) -> {
				component.getCommitButton().setEnabled(commitAllowed);
			});

			VaadinUiUtil.showModalPopupWindow(component, I18nProperties.getString(Strings.headingPickOrCreateContact));
			contactSelect.selectBestMatch();
		} else {
			createNewContact(contact, resultConsumer);
		}
	}

	private void createNewContact(ContactDto contact, Consumer<String> resultConsumer) {
		final ContactDto savedContact = FacadeProvider.getContactFacade().saveContact(contact);
		Notification.show(I18nProperties.getString(Strings.messageContactCreated), Type.WARNING_MESSAGE);
		resultConsumer.accept(savedContact.getUuid());
	}

	public CommitDiscardWrapperComponent<ContactDataForm> getContactDataEditComponent(
		String contactUuid,
		final ViewMode viewMode,
		boolean isPsuedonymized) {

		//editForm.setWidth(editForm.getWidth() * 8/12, Unit.PIXELS);
		ContactDto contact = FacadeProvider.getContactFacade().getContactByUuid(contactUuid);
		ContactDataForm editForm = new ContactDataForm(contact.getDisease(), viewMode, isPsuedonymized);
		editForm.setValue(contact);
		final CommitDiscardWrapperComponent<ContactDataForm> editComponent = new CommitDiscardWrapperComponent<ContactDataForm>(
			editForm,
			UserProvider.getCurrent().hasUserRight(UserRight.CONTACT_EDIT),
			editForm.getFieldGroup());

		editComponent.addCommitListener(new CommitListener() {

			@Override
			public void onCommit() {
				if (!editForm.getFieldGroup().isModified()) {
					ContactDto dto = editForm.getValue();

					// set the contact person's address to the one of the case when it is currently empty and
					// the relationship with the case has been set to living in the same household
					if (dto.getRelationToCase() == ContactRelation.SAME_HOUSEHOLD && dto.getCaze() != null) {
						PersonDto person = FacadeProvider.getPersonFacade().getPersonByUuid(dto.getPerson().getUuid());
						if (person.getAddress().isEmptyLocation()) {
							CaseDataDto caze = FacadeProvider.getCaseFacade().getCaseDataByUuid(dto.getCaze().getUuid());
							person.getAddress().setRegion(caze.getRegion());
							person.getAddress().setDistrict(caze.getDistrict());
							person.getAddress().setCommunity(caze.getCommunity());
						}
						FacadeProvider.getPersonFacade().savePerson(person);
					}

					dto = FacadeProvider.getContactFacade().saveContact(dto);
					Notification.show(I18nProperties.getString(Strings.messageContactSaved), Type.WARNING_MESSAGE);
					SormasUI.refreshView();
				}
			}
		});

		if (UserProvider.getCurrent().hasUserRole(UserRole.ADMIN)) {
			editComponent.addDeleteListener(() -> {
				FacadeProvider.getContactFacade().deleteContact(contact.getUuid());
				UI.getCurrent().getNavigator().navigateTo(ContactsView.VIEW_NAME);
			}, I18nProperties.getString(Strings.entityContact));
		}

		return editComponent;
	}

	public void showBulkContactDataEditComponent(Collection<? extends ContactIndexDto> selectedContacts, String caseUuid) {
		if (selectedContacts.size() == 0) {
			new Notification(
				I18nProperties.getString(Strings.headingNoContactsSelected),
				I18nProperties.getString(Strings.messageNoContactsSelected),
				Type.WARNING_MESSAGE,
				false).show(Page.getCurrent());
			return;
		}

		// Check if cases with multiple districts have been selected
		String districtUuid = null;
		for (ContactIndexDto selectedContact : selectedContacts) {
			if (districtUuid == null) {
				districtUuid = selectedContact.getDistrictUuid();
			} else if (!districtUuid.equals(selectedContact.getDistrictUuid())) {
				districtUuid = null;
				break;
			}
		}

		DistrictReferenceDto district = districtUuid != null ? FacadeProvider.getDistrictFacade().getDistrictReferenceByUuid(districtUuid) : null;

		// Create a temporary contact in order to use the CommitDiscardWrapperComponent
		ContactBulkEditData bulkEditData = new ContactBulkEditData();
		BulkContactDataForm form = new BulkContactDataForm(district);
		form.setValue(bulkEditData);
		final CommitDiscardWrapperComponent<BulkContactDataForm> editView =
			new CommitDiscardWrapperComponent<BulkContactDataForm>(form, form.getFieldGroup());

		Window popupWindow = VaadinUiUtil.showModalPopupWindow(editView, I18nProperties.getString(Strings.headingEditContacts));

		editView.addCommitListener(new CommitListener() {

			@Override
			public void onCommit() {
				ContactBulkEditData updatedBulkEditData = form.getValue();
				for (ContactIndexDto indexDto : selectedContacts) {
					ContactDto contactDto = FacadeProvider.getContactFacade().getContactByUuid(indexDto.getUuid());
					if (form.getClassificationCheckBox().getValue() == true) {
						contactDto.setContactClassification(updatedBulkEditData.getContactClassification());
					}
					// Setting the contact officer is only allowed if all selected contacts are in the same district
					if (district != null && form.getContactOfficerCheckBox().getValue() == true) {
						contactDto.setContactOfficer(updatedBulkEditData.getContactOfficer());
					}

					FacadeProvider.getContactFacade().saveContact(contactDto);
				}
				popupWindow.close();
				if (caseUuid == null) {
					overview();
				} else {
					caseContactsOverview(caseUuid);
				}
				Notification.show(I18nProperties.getString(Strings.messageContactsEdited), Type.HUMANIZED_MESSAGE);
			}
		});

		editView.addDiscardListener(() -> popupWindow.close());
	}

	public void deleteAllSelectedItems(Collection<? extends ContactIndexDto> selectedRows, Runnable callback) {
		if (selectedRows.size() == 0) {
			new Notification(
				I18nProperties.getString(Strings.headingNoContactsSelected),
				I18nProperties.getString(Strings.messageNoContactsSelected),
				Type.WARNING_MESSAGE,
				false).show(Page.getCurrent());
		} else {
			VaadinUiUtil.showDeleteConfirmationWindow(
				String.format(I18nProperties.getString(Strings.confirmationDeleteContacts), selectedRows.size()),
				new Runnable() {

					public void run() {
						for (ContactIndexDto selectedRow : selectedRows) {
							FacadeProvider.getContactFacade().deleteContact(selectedRow.getUuid());
						}
						callback.run();
						new Notification(
							I18nProperties.getString(Strings.headingContactsDeleted),
							I18nProperties.getString(Strings.messageContactsDeleted),
							Type.HUMANIZED_MESSAGE,
							false).show(Page.getCurrent());
					}
				});
		}
	}

	public void cancelFollowUpOfAllSelectedItems(Collection<? extends ContactIndexDto> selectedRows, Runnable callback) {

		if (selectedRows.size() == 0) {
			new Notification(
				I18nProperties.getString(Strings.headingNoContactsSelected),
				I18nProperties.getString(Strings.messageNoContactsSelected),
				Type.WARNING_MESSAGE,
				false).show(Page.getCurrent());
		} else {
			VaadinUiUtil.showDeleteConfirmationWindow(
				String.format(I18nProperties.getString(Strings.confirmationCancelFollowUp), selectedRows.size()),
				new Runnable() {

					public void run() {
						for (ContactIndexDto contact : selectedRows) {
							if (contact.getFollowUpStatus() != FollowUpStatus.NO_FOLLOW_UP) {
								ContactDto contactDto = FacadeProvider.getContactFacade().getContactByUuid(contact.getUuid());
								contactDto.setFollowUpStatus(FollowUpStatus.CANCELED);
								contactDto.setFollowUpComment(
									String.format(I18nProperties.getString(Strings.infoCanceledBy), UserProvider.getCurrent().getUserName()));
								FacadeProvider.getContactFacade().saveContact(contactDto);
							}
						}
						callback.run();
						new Notification(
							I18nProperties.getString(Strings.headingFollowUpCanceled),
							I18nProperties.getString(Strings.messageFollowUpCanceled),
							Type.HUMANIZED_MESSAGE,
							false).show(Page.getCurrent());
					}
				});
		}
	}

	public void setAllSelectedItemsToLostToFollowUp(Collection<? extends ContactIndexDto> selectedRows, Runnable callback) {
		if (selectedRows.size() == 0) {
			new Notification(
				I18nProperties.getString(Strings.headingNoContactsSelected),
				I18nProperties.getString(Strings.messageNoContactsSelected),
				Type.WARNING_MESSAGE,
				false).show(Page.getCurrent());
		} else {
			VaadinUiUtil.showDeleteConfirmationWindow(
				String.format(I18nProperties.getString(Strings.confirmationLostToFollowUp), selectedRows.size()),
				new Runnable() {

					public void run() {
						for (ContactIndexDto contact : selectedRows) {
							if (contact.getFollowUpStatus() != FollowUpStatus.NO_FOLLOW_UP) {
								ContactDto contactDto = FacadeProvider.getContactFacade().getContactByUuid(contact.getUuid());
								contactDto.setFollowUpStatus(FollowUpStatus.LOST);
								contactDto.setFollowUpComment(
									String.format(I18nProperties.getString(Strings.infoLostToFollowUpBy), UserProvider.getCurrent().getUserName()));
								FacadeProvider.getContactFacade().saveContact(contactDto);
							}
						}
						callback.run();
						new Notification(
							I18nProperties.getString(Strings.headingFollowUpStatusChanged),
							I18nProperties.getString(Strings.messageFollowUpStatusChanged),
							Type.HUMANIZED_MESSAGE,
							false).show(Page.getCurrent());
					}
				});
		}
	}

	public void openSelectCaseForContactWindow(Disease disease, Consumer<CaseIndexDto> selectedCaseCallback) {

		CaseCriteria criteria = new CaseCriteria().disease(disease);
		CaseSelectionField selectionField = new CaseSelectionField(criteria);
		selectionField.setWidth(1280, Unit.PIXELS);

		final CommitDiscardWrapperComponent<CaseSelectionField> component = new CommitDiscardWrapperComponent<>(selectionField);
		component.getCommitButton().setCaption(I18nProperties.getCaption(Captions.actionConfirm));
		component.getCommitButton().setEnabled(false);
		component.addCommitListener(() -> {
			selectedCaseCallback.accept(selectionField.getValue());
		});

		selectionField.setSelectionChangeCallback((commitAllowed) -> {
			component.getCommitButton().setEnabled(commitAllowed);
		});

		VaadinUiUtil.showModalPopupWindow(component, I18nProperties.getString(Strings.headingSelectSourceCase));
	}

	/**
	 * Opens a window that contains an iFrame with the symptom journal website specified in the properties.
	 * The steps to build that iFrame are:
	 * 1. Request an authentication token based on the stored client ID and secret
	 * 2. Build an HTML page containing a form with the auth token and some personal details as parameters
	 * 3. The form is automatically submitted and replaced by the iFrame
	 */
	public void openSymptomJournalWindow(PersonDto person) {
		String authToken = getSymptomJournalAuthToken();
		BrowserFrame frame = new BrowserFrame(null, new StreamResource(() -> {
			String formUrl = FacadeProvider.getConfigFacade().getSymptomJournalUrl();
			Map<String, String> parameters = new LinkedHashMap<>();
			parameters.put("token", authToken);
			parameters.put("uuid", person.getUuid());
			parameters.put("firstname", person.getFirstName());
			parameters.put("lastname", person.getLastName());
			parameters.put("email", person.getEmailAddress());
			byte[] document = createSymptomJournalForm(formUrl, parameters);

			return new ByteArrayInputStream(document);
		}, "symptomJournal.html"));
		frame.setWidth("100%");
		frame.setHeight("100%");

		Window window = VaadinUiUtil.createPopupWindow();
		window.setContent(frame);
		window.setCaption(I18nProperties.getString(Strings.headingPIAAccountCreation));
		window.setWidth(80, Unit.PERCENTAGE);
		window.setHeight(80, Unit.PERCENTAGE);

		UI.getCurrent().addWindow(window);
	}

	/**
	 * Opens a new tab addressing the climedo server specified in the sormas.properties.
	 * The current person is specified in the url, it is left to climedo to decide what to do with that information.
	 */
	public void openDiaryTab(PersonDto person) {
		String url = FacadeProvider.getConfigFacade().getPatientDiaryUrl();
		url += "/enroll?personUuid=" + person.getUuid();
		UI.getCurrent().getPage().open(url, "_blank");
	}

	private String getSymptomJournalAuthToken() {
		String authenticationUrl = FacadeProvider.getConfigFacade().getSymptomJournalAuthUrl();
		String clientId = FacadeProvider.getConfigFacade().getSymptomJournalClientId();
		String secret = FacadeProvider.getConfigFacade().getSymptomJournalSecret();

		if (StringUtils.isBlank(authenticationUrl)) {
			throw new IllegalArgumentException("Property interface.symptomjournal.authurl is not defined");
		}
		if (StringUtils.isBlank(clientId)) {
			throw new IllegalArgumentException("Property interface.symptomjournal.clientid is not defined");
		}
		if (StringUtils.isBlank(secret)) {
			throw new IllegalArgumentException("Property interface.symptomjournal.secret is not defined");
		}

		try {
			Client client = ClientBuilder.newClient();
			HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(clientId, secret);
			client.register(feature);
			WebTarget webTarget = client.target(authenticationUrl);
			Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
			Response response = invocationBuilder.post(Entity.json(""));
			String responseJson = response.readEntity(String.class);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readValue(responseJson, JsonNode.class);
			return node.get("auth").textValue();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * @return An HTML page containing a form that is automatically submitted in order to display the symptom journal iFrame
	 */
	private byte[] createSymptomJournalForm(String formUrl, Map<String, String> inputs) {
		Document document;
		try (InputStream in = getClass().getResourceAsStream("/symptomJournal.html")) {
			document = Jsoup.parse(in, StandardCharsets.UTF_8.name(), FacadeProvider.getConfigFacade().getSymptomJournalUrl());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Element form = document.getElementById("form");
		form.attr("action", formUrl);
		Element parametersElement = form.getElementById("parameters");

		inputs.forEach((k, v) -> parametersElement.appendChild(new Element("input").attr("type", "hidden").attr("name", k).attr("value", v)));
		return document.toString().getBytes(StandardCharsets.UTF_8);
	}

	public CommitDiscardWrapperComponent<EpiDataForm> getEpiDataComponent(final String contactUuid) {

		ContactDto contact = FacadeProvider.getContactFacade().getContactByUuid(contactUuid);
		EpiDataForm epiDataForm = new EpiDataForm(contact.getDisease(), contact.getEpiData().isPseudonymized());
		epiDataForm.setValue(contact.getEpiData());

		final CommitDiscardWrapperComponent<EpiDataForm> editView = new CommitDiscardWrapperComponent<EpiDataForm>(
			epiDataForm,
			UserProvider.getCurrent().hasUserRight(UserRight.CONTACT_EDIT),
			epiDataForm.getFieldGroup());

		editView.addCommitListener(() -> {
			ContactDto contactDto = FacadeProvider.getContactFacade().getContactByUuid(contactUuid);
			contactDto.setEpiData(epiDataForm.getValue());
			FacadeProvider.getContactFacade().saveContact(contactDto);
			Notification.show(I18nProperties.getString(Strings.messageContactSaved), Type.WARNING_MESSAGE);
			SormasUI.refreshView();
		});

		return editView;
	}
}
