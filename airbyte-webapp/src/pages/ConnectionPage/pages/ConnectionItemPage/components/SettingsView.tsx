import React, { useState } from "react";
import { FormattedMessage } from "react-intl";
import styled from "styled-components";

import ContentCard from "../../../../../components/ContentCard";
import { Connection } from "../../../../../core/resources/Connection";
import FrequencyConfig from "../../../../../data/FrequencyConfig.json";
import useConnection from "../../../../../components/hooks/services/useConnectionHook";
import DeleteBlock from "../../../../../components/DeleteBlock";
import FrequencyForm from "../../../../../components/FrequencyForm";

type IProps = {
  connection: Connection;
  onAfterSaveSchema: () => void;
};

const Content = styled.div`
  max-width: 813px;
  margin: 18px auto;
`;

const SettingsView: React.FC<IProps> = ({ connection, onAfterSaveSchema }) => {
  const [saved, setSaved] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const { updateConnection, deleteConnection } = useConnection();

  const schedule = FrequencyConfig.find(
    item => JSON.stringify(item.config) === JSON.stringify(connection.schedule)
  );

  const onSubmit = async (
    values: { frequency: string },
    checkedState: string[]
  ) => {
    const frequencyData = FrequencyConfig.find(
      item => item.value === values.frequency
    );

    const result = await updateConnection({
      connectionId: connection.connectionId,
      syncSchema: connection.syncSchema, //todo: fix
      status: connection.status,
      schedule: frequencyData?.config || null
    });

    if (result.status === "failure") {
      setErrorMessage(result.message);
    } else {
      setSaved(true);
      //todo: fix
      if (JSON.stringify(checkedState) !== JSON.stringify([])) {
        onAfterSaveSchema();
      }
    }
  };

  const onDelete = () => {
    deleteConnection({ connectionId: connection.connectionId });
  };

  return (
    <Content>
      <ContentCard
        title={<FormattedMessage id="connection.connectionSettings" />}
      >
        <FrequencyForm
          isEditMode
          schema={connection.syncSchema}
          onSubmit={onSubmit}
          frequencyValue={schedule?.value}
          errorMessage={errorMessage}
          successMessage={saved && <FormattedMessage id="form.changesSaved" />}
        />
      </ContentCard>
      {/* TODO: fix on delete*/}
      <DeleteBlock type="connection" onDelete={onDelete} />
    </Content>
  );
};

export default SettingsView;
