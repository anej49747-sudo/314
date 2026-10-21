// Keep the Story #14 imports stable while sharing the rules with Story #21.
export {
  buildAccountSettingsPayload as buildDesignerAccountSettingsPayload,
  accountSettingsChanged as designerAccountSettingsChanged,
  accountSettingsFailureMessage as designerAccountSettingsFailureMessage,
} from '../account/accountSettingsRules.js'
